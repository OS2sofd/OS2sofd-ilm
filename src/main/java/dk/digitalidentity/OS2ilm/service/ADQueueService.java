package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.exception.ADOperationException;
import dk.digitalidentity.OS2ilm.exception.RetryableException;
import dk.digitalidentity.OS2ilm.model.entity.AdGroup;
import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.EmailTemplate;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplateType;
import dk.digitalidentity.OS2ilm.model.entity.enums.AccountNameStrategy;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.queue.QueueConstants;
import dk.digitalidentity.OS2ilm.queue.RetryableMessageHandler;
import dk.digitalidentity.simple_queue.QueueMessage;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static dk.digitalidentity.OS2ilm.util.QueueUtils.objectToJson;

@Slf4j
@Service
public class ADQueueService extends RetryableMessageHandler {
	private static final String QUEUE_IDENTIFIER = QueueConstants.AD_QUEUE;
	private static final int MAX_ATTEMPTS = 3;

	private final EmployeeService employeeService;
	private final ObjectMapper mapper;
	private final ActiveDirectorySyncService activeDirectorySyncService;
	private final AdGroupService adGroupService;
	private final QueueHelperService queueHelperService;
	private final AuditLogService auditLogService;
	private final EmailTemplateService emailTemplateService;
	private final MessageService messageService;
	private final EncryptionUtil encryptionUtil;
	private final AccountNameTemplateService accountNameTemplateService;

	public ADQueueService(ApplicationEventPublisher eventPublisher,
			EmployeeService employeeService, ObjectMapper mapper,
			ActiveDirectorySyncService activeDirectorySyncService,
			AdGroupService adGroupService, QueueHelperService queueHelperService, AuditLogService auditLogService, EmailTemplateService emailTemplateService, MessageService messageService, EncryptionUtil encryptionUtil,
			AccountNameTemplateService accountNameTemplateService
	) {
		super(QUEUE_IDENTIFIER, MAX_ATTEMPTS, eventPublisher);
		this.employeeService = employeeService;
		this.mapper = mapper;
		this.activeDirectorySyncService = activeDirectorySyncService;
		this.adGroupService = adGroupService;
		this.queueHelperService = queueHelperService;
		this.auditLogService = auditLogService;
		this.emailTemplateService = emailTemplateService;
		this.messageService = messageService;
		this.encryptionUtil = encryptionUtil;
		this.accountNameTemplateService = accountNameTemplateService;
	}

	public enum OperationType {CREATE, SET_GROUP_MEMBERSHIP, DELETE_GROUP_MEMBER_SHIP, SET_EXPIRE_DATE, SET_PASSWORD, DISABLE, DELETE}

	// #### Queue functionality #### //

	/**
	 * This is where we handle the message
	 * @param message
	 * @return Use Return true to remove message from queue, return false to keep it
	 */
	@Override
	public boolean handleMessage(QueueMessage message) {
		log.info("Processing queue message: priority={}", message.getPriority());
		log.debug("Queue message body length: {}", message.getBody() != null ? message.getBody().length() : 0);
		try {
			ADMessageRequest adMessageRequest = stringToObject(message.getBody());

			switch (adMessageRequest.operation) {
				case CREATE:
					Employee employee = employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(adMessageRequest.employeeId());
					if (employee == null) {
						log.warn("Employee not found in DB for id {} - keeping message in queue as history", adMessageRequest.employeeId());
						throw new IllegalArgumentException("Employee not found in DB for id " + adMessageRequest.employeeId() + " - keeping message in queue as history");
					}

					AdUser user = employee.getUser();
					Long empId = adMessageRequest.employeeId();

					// Guards one-time operations (group queueing, audit log) that must not repeat on retry
					boolean accountJustCreated = (user == null);

					if (user == null) {
						String name = getUsernameForAdAccount(employee, adMessageRequest.personUuid());
						user = activeDirectorySyncService.createAccount(empId, adMessageRequest.activationTimeFrameId(), name);
						auditLogService.log(null, "System", empId, employee.getName(), LogAction.AD_USER_CREATED, "Ny bruger oprettet i AD", user.getName());
					}

					// If the employee has a stopDate set, we tell AD about it - this runs for every retry, which is fine
					if (employee.getStopDate() != null) {
						activeDirectorySyncService.setExpireDate(empId, employee.getStopDate());
					}

					// Password is generated once when the CREATE message is enqueued and carried in the message,
					// so retries use the same value, and it remains available for the welcome notification.
					String password = encryptionUtil.decryptPassword(adMessageRequest.password());
					if (user != null && !user.isInitialPasswordSet()) {
						activeDirectorySyncService.setPassword(empId, password, false);
					}

					// If the employee has a phone number we let AD know so that SOFD hears about it.
					String phoneNumber = employee.getPhone();
					if (phoneNumber != null && !phoneNumber.isBlank()) {
						activeDirectorySyncService.setPhoneAttribute(empId, phoneNumber);
					}

					// Let AD know about the business that the ad-account is part of.
					Business business = employee.getBusiness();
					if (business != null) {
						activeDirectorySyncService.setCompanyAttribute(empId, business.getId());
					}

					// Track whether setCprAttribute commits on this pass — it is the final sub-step,
					// so its success means all steps are now complete and the notification is due.
					boolean cprJustSet = false;
					if (user != null && user.getCpr() == null) {
						activeDirectorySyncService.setCprAttribute(empId, employee.getCpr());
						cprJustSet = true;
					}

					// AD user exists at this point (created above or on a previous attempt).
					// Group queueing and SOFD sync are best-effort — failures are logged but must not
					// cause a retry of the entire CREATE, which would duplicate group messages.
					if (accountJustCreated && adMessageRequest.groupIds() != null && !adMessageRequest.groupIds().isEmpty()) {
						log.info("Queueing {} group memberships for employee {}", adMessageRequest.groupIds().size(), empId);

						for (Long groupId : adMessageRequest.groupIds()) {
							if (!addSetGroupMembershipToQueue(empId, groupId)) {
								log.error("Failed to queue group membership for employee {}, group {} - group will be missing",
										empId, groupId);
								auditLogService.log(null, "System", empId, employee.getName(),
										LogAction.QUEUE_FAILED, "Kunne ikke tilføje gruppe " + groupId + " til kø efter oprettelse", empId);
							}
						}
					}

					// Send welcome notification on the pass where setCprAttribute commits (all steps done).
					if (cprJustSet) {
						EmailTemplate emailTemplate = emailTemplateService.getByType(EmailTemplateType.EMPLOYEE_APPROVED);
						if (emailTemplate.isEnabled()) {
							EmailTemplateService.RenderedEmail email = emailTemplateService.render(emailTemplate, Map.of(
									EmailTemplatePlaceholder.RECIPIENT, employee.getName(),
									EmailTemplatePlaceholder.USERNAME, user.getName(),
									EmailTemplatePlaceholder.PASSWORD, password));

							messageService.sendNotification(employee, email);
						}
					}

					// SOFD auto-discovers AD accounts via the CPR attribute — no need to push AD user data
					return CONSUMED;
				case SET_GROUP_MEMBERSHIP:
					activeDirectorySyncService.setGroupMembership(adMessageRequest.employeeId(), adMessageRequest.groupId);

					return CONSUMED;
				case DELETE_GROUP_MEMBER_SHIP:
					activeDirectorySyncService.deleteGroupMembership(adMessageRequest.employeeId(), adMessageRequest.groupId);

					return CONSUMED;
				case SET_EXPIRE_DATE:
					activeDirectorySyncService.setExpireDate(adMessageRequest.employeeId(), adMessageRequest.date);

					return CONSUMED;
				case SET_PASSWORD:
					boolean sendNotificationEnabled = true;
					activeDirectorySyncService.setPassword(adMessageRequest.employeeId(), encryptionUtil.decryptPassword(adMessageRequest.password), sendNotificationEnabled);

					return CONSUMED;
				case DISABLE:
					activeDirectorySyncService.disableAccount(adMessageRequest.adUserId());
					auditLogService.logSystemAction(adMessageRequest.employeeId(), adMessageRequest.employeeName(), LogAction.AD_USER_DISABLED, "AD-konto blev deaktiveret", adMessageRequest.adUserId());

					queueHelperService.sofdHelperForDeletion(adMessageRequest.employeeId(), adMessageRequest.performerId(), adMessageRequest.performerName());

					return CONSUMED;
				case DELETE:
					activeDirectorySyncService.deleteAccount(adMessageRequest.adUserId());
					auditLogService.logSystemAction(adMessageRequest.employeeId(), adMessageRequest.employeeName(), LogAction.AD_USER_DELETED, "AD-konto blev slettet", adMessageRequest.adUserId());

					queueHelperService.sofdHelperForDeletion(adMessageRequest.employeeId(), adMessageRequest.performerId(), adMessageRequest.performerName());

					return CONSUMED;
				default:
					throw new IllegalArgumentException();
			}
		}
		catch (ADOperationException e) {
			throw new RetryableException("AD operation failed, will retry", e);
		}
		catch (Exception e) {
			log.error("Unexpected error processing AD queue message", e);
			throw new RuntimeException("Unexpected error processing AD queue message", e);
		}
	}
	@Override
	protected boolean doHandleFailedMessage(QueueMessage message, Exception ex) {
		try {
			ADMessageRequest req = stringToObject(message.getBody());

			Employee employee = employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(req.employeeId());
			if (employee == null) {
				log.warn("Employee {} not found during compensation, skipping", req.employeeId());
				return FAIL_DEAD_LETTER;
			}
			switch (req.operation()) {
				case SET_GROUP_MEMBERSHIP -> {
					AdGroup group = adGroupService.findById(req.groupId());
					if (group != null && employee.getAdGroups().contains(group)) {
						employee.getAdGroups().remove(group);
						employeeService.save(employee);
						log.info("Removed group {} from employee {} after max retries exceeded", group.getId(), employee.getId());
					}
				}
				case DELETE_GROUP_MEMBER_SHIP -> {
					AdGroup group = adGroupService.findById(req.groupId());
					if (group != null && !employee.getAdGroups().contains(group)) {
						employee.addAdGroup(group);
						employeeService.save(employee);
						log.info("Re-added group {} to employee {} after max retries exceeded", group.getId(), employee.getId());
					}
				}
				case DISABLE -> {
					auditLogService.log(null, "System", req.employeeId(), req.employeeName(),
							LogAction.QUEUE_FAILED, "Permanent fejl i AD-kø for operation: " + req.operation(), req.adUserId());
					return FAIL_DEAD_LETTER;
				}
				default -> {} // CREATE, SET_PASSWORD, and SET_EXPIRE require no compensation
			}
			auditLogService.log(null, "System", req.employeeId(), employee.getName(),
					LogAction.QUEUE_FAILED, "Permanent fejl i AD-kø for operation: " + req.operation(), employee.getId());
		} catch (Exception e) {
			log.error("Compensation failed for permanently failed AD message", e);
		}
		return FAIL_DEAD_LETTER;
	}

	@Builder
	public record ADMessageRequest(
			OperationType operation,
			Long employeeId,
			Long activationTimeFrameId,
			Long groupId,        // For single group operations
			List<Long> groupIds, // For bulk group operations during creation
			LocalDate date,
			String password,
			String personUuid,
			Long adUserId,       // For DISABLE, where the employee may already be deleted
			String employeeName, // Snapshotted for logging DISABLE, where the employee may already be deleted
			String performerId,   // For DISABLE: the admin who triggered the deletion, carried through to finalizeEmployeeDeletion
			String performerName  // For DISABLE: the admin who triggered the deletion, carried through to finalizeEmployeeDeletion
	) { }

	private boolean publishToQueue(ADMessageRequest message) throws JacksonException {
		QueueMessage queueMessage = QueueMessage.builder()
				.queue(QUEUE_IDENTIFIER)
				.priority(1L)
				.body(objectToJson(message))
				.build();

		log.debug("Publishing queue message event: queue={}, operation={}, employeeId={}",
				QUEUE_IDENTIFIER, message.operation(), message.employeeId());

		eventPublisher.publishEvent(queueMessage);
		return true;
	}

	// #### Public Message Creation Methods #### //

	public boolean addSetGroupMembershipToQueue(Long employeeId, Long groupId) {
		try {
			ADMessageRequest message = ADMessageRequest.builder()
					.operation(OperationType.SET_GROUP_MEMBERSHIP)
					.employeeId(employeeId)
					.groupId(groupId)
					.build();
			return publishToQueue(message);
		} catch (Exception ex) {
			log.error("Failed to add SET_GROUP_MEMBERSHIP message to queue: employeeId={}, groupId={}",
					employeeId, groupId, ex);
			return false;
		}
	}

	public boolean addDeleteGroupMembershipToQueue(Long employeeId, Long groupId) {
		try {
			ADMessageRequest message = ADMessageRequest.builder()
					.operation(OperationType.DELETE_GROUP_MEMBER_SHIP)
					.employeeId(employeeId)
					.groupId(groupId)
					.build();
			return publishToQueue(message);
		} catch (Exception ex) {
			log.error("Failed to add DELETE_GROUP_MEMBERSHIP message to queue: employeeId={}, groupId={}",
					employeeId, groupId, ex);
			return false;
		}
	}

	public boolean addSetExpireDateToQueue(Long employeeId, LocalDate date) {
		try {
			ADMessageRequest message = ADMessageRequest.builder()
					.operation(OperationType.SET_EXPIRE_DATE)
					.employeeId(employeeId)
					.date(date)
					.build();
			return publishToQueue(message);
		} catch (Exception ex) {
			log.error("Failed to add SET_EXPIRE_DATE message to queue: employeeId={}, date={}",
					employeeId, date, ex);
			return false;
		}
	}

	public boolean addSetPasswordToQueue(Long employeeId, String password) {
		try {
			ADMessageRequest message = ADMessageRequest.builder()
					.operation(OperationType.SET_PASSWORD)
					.employeeId(employeeId)
					.password(encryptionUtil.encryptPassword(password))
					.build();
			return publishToQueue(message);
		} catch (Exception ex) {
			log.error("Failed to add SET_PASSWORD message to queue: employeeId={}", employeeId, ex);
			return false;
		}
	}

	public boolean addDisableAccountToQueue(Long employeeId, Long adUserId, String employeeName, String performerId, String performerName) {
		try {
			ADMessageRequest message = ADMessageRequest.builder()
					.operation(OperationType.DISABLE)
					.employeeId(employeeId)
					.adUserId(adUserId)
					.employeeName(employeeName)
					.performerId(performerId)
					.performerName(performerName)
					.build();
			return publishToQueue(message);
		} catch (Exception ex) {
			log.error("Failed to add DISABLE message to queue: employeeId={}, adUserId={}", employeeId, adUserId, ex);
			return false;
		}
	}

	public boolean addDeleteAccountToQueue(Long employeeId, Long adUserId, String employeeName, String performerId, String performerName) {
		try {
			ADMessageRequest message = ADMessageRequest.builder()
					.operation(OperationType.DELETE)
					.employeeId(employeeId)
					.adUserId(adUserId)
					.employeeName(employeeName)
					.performerId(performerId)
					.performerName(performerName)
					.build();
			return publishToQueue(message);
		} catch (Exception ex) {
			log.error("Failed to add DISABLE message to queue: employeeId={}, adUserId={}", employeeId, adUserId, ex);
			return false;
		}
	}

	// #### Helper methods #### //

	private ADMessageRequest stringToObject(String body) throws JacksonException {
		log.debug("Converting JSON string to object: length={}", body.length());
		return mapper.readValue(body, ADMessageRequest.class);
	}

	private String getUsernameForAdAccount(Employee employee, String personUuid) {
		AccountNameStrategy strategy = resolveStrategy(employee);

		// An admin-supplied override (e.g. after a naming collision) always takes precedence,
		// regardless of the business' naming strategy, so a retry doesn't just regenerate the same failing name.
		if (StringUtils.hasText(employee.getPendingAccountName())) {
			return employee.getPendingAccountName();
		}

		return switch (strategy) {
			case SOFD -> queueHelperService.sofdGenerationHelper(employee.getId(), personUuid);
			case PREFIXED -> activeDirectorySyncService.makeAdUserName(employee);
			case TAILORED -> accountNameTemplateService.resolve(getAccountNameTemplate(employee), employee);
			case CUSTOM -> throw new ADOperationException("Employee " + employee.getName() + " uses CUSTOM AD-name but has no account name set");
		};
	}

	private static String getAccountNameTemplate(Employee employee) {
		if (employee.getBusiness() != null && employee.getBusiness().getBusinessEmploymentSettings() != null) {
			return employee.getBusiness().getBusinessEmploymentSettings().getAccountNameTemplate();
		}
		throw new ADOperationException("Employee " + employee.getName() + " uses TAILORED AD-name but has no business employment settings configured");
	}

	private AccountNameStrategy resolveStrategy(Employee employee) {
		if (employee.getBusiness() != null && employee.getBusiness().getBusinessEmploymentSettings() != null
				&& employee.getBusiness().getBusinessEmploymentSettings().getNameStrategy() != null) {
			return employee.getBusiness().getBusinessEmploymentSettings().getNameStrategy();
		}
		return AccountNameStrategy.PREFIXED;
	}
}
