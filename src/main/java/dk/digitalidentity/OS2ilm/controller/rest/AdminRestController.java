package dk.digitalidentity.OS2ilm.controller.rest;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

import dk.digitalidentity.OS2ilm.model.entity.CmsMessage;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.dto.CmsMessageRecord;
import dk.digitalidentity.OS2ilm.model.entity.EmailTemplate;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplateType;
import dk.digitalidentity.OS2ilm.model.entity.enums.CmsProperty;
import dk.digitalidentity.OS2ilm.model.entity.enums.CmsStatus;
import dk.digitalidentity.OS2ilm.model.exception.InvalidAccountNameException;
import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.service.AccountNameValidator;
import dk.digitalidentity.OS2ilm.service.AdUserService;
import dk.digitalidentity.OS2ilm.service.EmailTemplateService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.MessageService;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import dk.digitalidentity.OS2ilm.model.dao.SimpleQueueItemDao;
import dk.digitalidentity.OS2ilm.service.ADQueueService;
import dk.digitalidentity.OS2ilm.queue.QueueConstants;
import dk.digitalidentity.simple_queue.entity.QueueHeader;
import dk.digitalidentity.simple_queue.entity.QueueItem;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import dk.digitalidentity.OS2ilm.model.datatable.dao.ActiveDirectoryDatatableDao;
import dk.digitalidentity.OS2ilm.model.datatable.dao.AuditLogDatatableDao;
import dk.digitalidentity.OS2ilm.model.entity.ActivationTimeFrame;
import dk.digitalidentity.OS2ilm.model.entity.OUTreeForm;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.model.role.RequireReadOnlyAdministrator;
import dk.digitalidentity.OS2ilm.model.role.RequireSystemAdministrator;
import dk.digitalidentity.OS2ilm.model.view.AdUserView;
import dk.digitalidentity.OS2ilm.model.view.AuditLogView;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.ActivationTimeFrameService;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.OrgUnitService;
import dk.digitalidentity.OS2ilm.service.cms.CmsMessageBundle;
import dk.digitalidentity.OS2ilm.service.cms.CmsMessageService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Hidden
@RequestMapping("/rest/admin")
@RestController
@RequiredArgsConstructor
@RequireReadOnlyAdministrator
public class AdminRestController {
	public static final String CMS_EDIT_PAGE = "admin/cms/edit";
	private final AuditLogService auditLogService;
	private final CmsMessageService cmsMessageService;
	private final CmsMessageBundle cmsMessageBundle;
	private final AuditLogDatatableDao auditLogDatatableDao;
	private final OrgUnitService orgUnitService;
	private final ActivationTimeFrameService activationTimeFrameService;
	private final SecurityUtil securityUtil;
	private final ActiveDirectoryDatatableDao activeDirectoryDatatableDao;
	private final SimpleQueueItemDao queueItemDao;
	private final ObjectMapper objectMapper;
	private final MessageService messageService;
	private final IlmConfiguration configuration;
	private final EmployeeService employeeService;
	private final AccountNameValidator accountNameValidator;
	private final AdUserService adUserService;
	private final EmailTemplateService emailTemplateService;

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

	public record CmsMessageLog(Long id, String cmsKey, String cmsValue, LocalDateTime lastUpdated) {}
	@PostMapping("/download/log/yearly")
	public ResponseEntity<StreamingResponseBody> downloadLogYear(HttpServletRequest request, HttpServletResponse response) {
		return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=\"AuditLog.zip\"").body(out -> {
			var zipOutputStream = new ZipOutputStream(out);
			// Call toCSV here
			auditLogService.addFilesToZipCsv(zipOutputStream, "auditlog", "year");
			zipOutputStream.close();
		});
	}

	@PostMapping("/download/log/monthly")
	public ResponseEntity<StreamingResponseBody> downloadLogMonth(HttpServletRequest request, HttpServletResponse response) {
		return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=\"AuditLog.zip\"").body(out -> {
			var zipOutputStream = new ZipOutputStream(out);
			// Call toCSV here
			auditLogService.addFilesToZipCsv(zipOutputStream, "auditlog", "month");
			zipOutputStream.close();
		});
	}

	@RequireSystemAdministrator
	@GetMapping("/admin/cms-edit")
	public String editCms(Model model, @RequestParam("key") String key) {
		CmsMessageRecord dto = new CmsMessageRecord(key, cmsMessageBundle.getText(key, true), cmsMessageBundle.getDescription(key));
		populateModel(model, dto, false);

		return CMS_EDIT_PAGE;
	}

	@RequireSystemAdministrator
	@PostMapping("/cms-edit")
	public String saveCms(Model model, CmsMessageRecord cmsMessage) {
		if (cmsMessage.value() != null && cmsMessage.value().length() > 65536) {
			CmsMessageRecord dto = new CmsMessageRecord(cmsMessage.key(), cmsMessage.value(), cmsMessageBundle.getDescription(cmsMessage.key()));
			populateModel(model, dto, true);

			return CMS_EDIT_PAGE;
		}

		PolicyFactory policy = new HtmlPolicyBuilder()
				.allowCommonBlockElements()
				.allowCommonInlineFormattingElements()
				.allowElements("a")
				.allowUrlProtocols("https")
				.allowAttributes("href", "target").onElements("a")
				.allowAttributes("class", "style", "id", "name").globally()
				.toFactory();
		String safeHTML = policy.sanitize(cmsMessage.value());

		CmsMessage cms = cmsMessageService.getByCmsKey(cmsMessage.key());

		if (cms == null) {
			cms = new CmsMessage();
			cms.setCmsKey(cmsMessage.key());
		}

		cms.setCmsValue(safeHTML);
		cms.setLastUpdated(LocalDateTime.now());
		cms = cmsMessageService.save(cms);

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), cms.getId(), "CMS", LogAction.ADDED_CMS_MESSAGE, "CMS besked tilføjet!", new CmsMessageLog(cms.getId(), cms.getCmsKey(), cms.getCmsValue(), cms.getLastUpdated()));

		return "redirect:/admin/cms";
	}

	@RequireSystemAdministrator
	@PostMapping("/cms-edit/logo")
	public String saveCms(@RequestParam("value") MultipartFile file, @RequestParam("key") String key, Model model) throws IOException {
		String base64 = Base64.getEncoder().encodeToString(file.getBytes());

		CmsMessage logo = cmsMessageService.getByCmsKey(CmsProperty.LOGO.getKey());

		if (logo == null) {
			logo = new CmsMessage();
			logo.setCmsKey(CmsProperty.LOGO.getKey());
		}

		if (base64.length() > 65536) {
			CmsMessageRecord dto = new CmsMessageRecord(logo.getCmsKey(), null, cmsMessageBundle.getDescription(logo.getCmsKey()));
			populateModel(model, dto, true);

			return CMS_EDIT_PAGE;
		}

		logo.setCmsValue(base64);
		logo.setLastUpdated(LocalDateTime.now());
		cmsMessageService.save(logo);

		return "redirect:/admin/cms";
	}

	private static void populateModel(Model model, CmsMessageRecord dto, boolean showErrors) {
		model.addAttribute("cmsMessage", dto);
		model.addAttribute("showError", showErrors);
	}

	@RequireSystemAdministrator
	@PostMapping("/cms/delete/{id}")
	public ResponseEntity<String> deleteCms(@PathVariable String id) {
		CmsMessage cms = cmsMessageService.getByCmsKey(id);

		if (cms != null) {
			// Instead of deleting we set it to null as it is easier to update across servers
			cms.setCmsValue(null);
			cms.setLastUpdated(LocalDateTime.now());
			cms = cmsMessageService.save(cms);

			String performerId = securityUtil.getPerformerId();
			auditLogService.log(performerId, securityUtil.getName(), cms.getId(), "CMS", LogAction.DELETED_CMS_MESSAGE, "CMS besked slettet", new CmsMessageLog(cms.getId(), cms.getCmsKey(), cms.getCmsValue(), cms.getLastUpdated()));
		}

		return ResponseEntity.ok().build();
	}

	public record SetCmsActiveRecord(String key, String text, boolean value) {}
	@RequireSystemAdministrator
	@PostMapping("/cms/enable")
	public ResponseEntity<String> enableCms(@RequestBody SetCmsActiveRecord record) {
		CmsMessage cms = cmsMessageService.getByCmsKey(record.key);
		if (cms != null) {
			cms.setActive(record.value);
			cms.setCmsStatus(record.value ? CmsStatus.ACTIVE : CmsStatus.INACTIVE);
			cms.setLastUpdated(LocalDateTime.now());
		} else {
			cms = new CmsMessage();
			cms.setCmsKey(record.key);
			cms.setCmsValue(record.text);
			cms.setActive(record.value);
			cms.setCmsStatus(record.value ? CmsStatus.ACTIVE : CmsStatus.INACTIVE);
		}

		CmsMessage saved = cmsMessageService.save(cms);

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), saved.getId(), "CMS", LogAction.ACTIVATED_CMS_MESSAGE, "CMS besked aktiveret", new CmsMessageLog(saved.getId(), saved.getCmsKey(), saved.getCmsValue(), saved.getLastUpdated()));
		return ResponseEntity.ok().build();
	}

	public record AuditLogViewDTO(long id, LocalDateTime tts, String targetId, String targetName, String performerId, String performerName, String logAction, String message, Long detailId) {}

	@PostMapping("/auditlog/view")
	public DataTablesOutput<AuditLogViewDTO> paginatingTable(@RequestBody DataTablesInput input) {
		DataTablesOutput<AuditLogView> all = auditLogDatatableDao.findAll(input);
		return convertAuditLogDataTablesModelToDTO(all);
	}

	private DataTablesOutput<AuditLogViewDTO> convertAuditLogDataTablesModelToDTO(DataTablesOutput<AuditLogView> output) {
		List<AuditLogViewDTO> dataWithMessages = output.getData().stream()
				.map(auditlog -> new AuditLogViewDTO(auditlog.getId(), auditlog.getTts(), auditlog.getTargetId(), auditlog.getTargetName(), auditlog.getPerformerId(), auditlog.getPerformerName(), auditlog.getLogAction().getType().getTranslation(), auditlog.getMessage(), auditlog.getDetailId()))
				.collect(Collectors.toCollection(ArrayList::new));

		DataTablesOutput<AuditLogViewDTO> result = new DataTablesOutput<>();
		result.setData(dataWithMessages);
		result.setDraw(output.getDraw());
		result.setError(output.getError());
		result.setRecordsFiltered(output.getRecordsFiltered());
		result.setRecordsTotal(output.getRecordsTotal());

		return result;
	}

	@RequireSystemAdministrator
	@PostMapping("/orgUnits/tree")
	public List<OUTreeForm> getOrgUnitTree() {
		return orgUnitService.getAllTree();
	}

	public record CreateActivation(LocalTime startTime, LocalTime stopTime, boolean excludeWeekends, String name) {}
	public record CreateActivationLog(Long id, String name, LocalTime startTime, LocalTime stopTime, boolean excludeWeekends) {}
	@RequireSystemAdministrator
	@PostMapping("/activation/create")
	public ResponseEntity<?> createActivation(@RequestBody CreateActivation activationRecord) {
		ActivationTimeFrame activation = new ActivationTimeFrame();
		activation.setName(activationRecord.name);
		activation.setStartTime(activationRecord.startTime);
		activation.setStopTime(activationRecord.stopTime);
		activation.setExcludeWeekends(activationRecord.excludeWeekends);

		ActivationTimeFrame saved = activationTimeFrameService.save(activation);

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), saved.getId(), saved.getName(), LogAction.ACTIVATION_TIME_FRAME_CREATED, "Aktiveringsperiode blev oprettet", new CreateActivationLog(saved.getId(), saved.getName(), saved.getStartTime(), saved.getStopTime(), saved.isExcludeWeekends()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireSystemAdministrator
	@PostMapping("/activation/edit/{id}")
	public ResponseEntity<?> editActivation(@PathVariable long id, @RequestBody CreateActivation activationRecord) {
		ActivationTimeFrame activation = activationTimeFrameService.findById(id);
		if (activation == null) {
			log.warn("Activation with id {} not found", id);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		activation.setName(activationRecord.name);
		activation.setStartTime(activationRecord.startTime);
		activation.setStopTime(activationRecord.stopTime);
		activation.setExcludeWeekends(activationRecord.excludeWeekends);

		ActivationTimeFrame saved = activationTimeFrameService.save(activation);

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), saved.getId(), saved.getName(), LogAction.ACTIVATION_TIME_FRAME_UPDATED, "Aktiveringsperiode blev opdateret", new CreateActivationLog(saved.getId(), saved.getName(), saved.getStartTime(), saved.getStopTime(), saved.isExcludeWeekends()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireSystemAdministrator
	@DeleteMapping("/activation/delete/{id}")
	public ResponseEntity<?> deleteActivation(@PathVariable long id) {
		ActivationTimeFrame activation = activationTimeFrameService.findById(id);
		if (activation == null) {
			log.warn("Activation with id {} not found", id);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		CreateActivationLog deletedActivation = new CreateActivationLog(activation.getId(), activation.getName(), activation.getStartTime(), activation.getStopTime(), activation.isExcludeWeekends());

		activationTimeFrameService.delete(activation);
		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), id, deletedActivation.name, LogAction.ACTIVATION_TIME_FRAME_DELETED, "Aktiveringsperiode blev slettet", deletedActivation);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/adAccounts/view")
	public DataTablesOutput<AdUserView> getAdUserView(@RequestBody DataTablesInput input) {
		return activeDirectoryDatatableDao.findAll(input);
	}

	public record FailedQueueItemDTO(long queueItemId, Long employeeId, String operation, Instant createdAt, String stackTrace, String nameStrategy) {}

	@GetMapping("/adQueue/failed")
	public List<FailedQueueItemDTO> getFailedAdQueueItems() {
		return queueItemDao.findAllByQueueAndStatus(QueueConstants.AD_QUEUE, QueueItem.QueueItemStatus.FAILED).stream()
				.map(this::toFailedQueueItemDTO)
				.toList();
	}

	private FailedQueueItemDTO toFailedQueueItemDTO(QueueItem item) {
		ADQueueService.ADMessageRequest request = null;
		try {
			request = objectMapper.readValue(item.getBody(), ADQueueService.ADMessageRequest.class);
		} catch (Exception e) {
			log.warn("Failed to parse queue item body for item {}", item.getId());
		}
		String stackTrace = item.getHeaders() == null ? null : item.getHeaders().stream()
				.filter(h -> dk.digitalidentity.simple_queue.QueueConstants.STACKTRACE_NAME.equals(h.getName()))
				.map(QueueHeader::getValue)
				.findFirst()
				.orElse(null);
		Long employeeId = request != null ? request.employeeId() : null;
		String operation = request != null ? request.operation().name() : null;
		String nameStrategy = null;
		if ("CREATE".equals(operation) && employeeId != null) {
			Employee employee = employeeService.findById(employeeId);
			if (employee != null && employee.getBusiness() != null && employee.getBusiness().getBusinessEmploymentSettings() != null
					&& employee.getBusiness().getBusinessEmploymentSettings().getNameStrategy() != null) {
				nameStrategy = employee.getBusiness().getBusinessEmploymentSettings().getNameStrategy().name();
			}
		}
		return new FailedQueueItemDTO(item.getId(), employeeId, operation, item.getCreatedAt(), stackTrace, nameStrategy);
	}

	@RequireSystemAdministrator
	@PostMapping("/adQueue/{id}/retry")
	public ResponseEntity<?> retryAdQueueItem(@PathVariable long id) {
		return queueItemDao.findById(id).map(item -> {
			if (item.getStatus() != QueueItem.QueueItemStatus.FAILED) {
				return ResponseEntity.status(HttpStatus.CONFLICT).build();
			}
			rearmQueueItem(item);
			return ResponseEntity.ok().build();
		}).orElse(ResponseEntity.notFound().build());
	}

	public record RetryWithNewUsernameRequest(String newUsername) {}

	@RequireSystemAdministrator
	@Transactional
	@PostMapping("/adQueue/{id}/retryWithNewUsername")
	public ResponseEntity<?> retryAdQueueItemWithNewUsername(@PathVariable long id, @RequestBody RetryWithNewUsernameRequest request) {
		return queueItemDao.findById(id).map(item -> {
			if (item.getStatus() != QueueItem.QueueItemStatus.FAILED) {
				return ResponseEntity.status(HttpStatus.CONFLICT).build();
			}

			ADQueueService.ADMessageRequest adMessageRequest;
			try {
				adMessageRequest = objectMapper.readValue(item.getBody(), ADQueueService.ADMessageRequest.class);
			} catch (Exception e) {
				return ResponseEntity.badRequest().body("Kunne ikke læse køelementet");
			}

			if (adMessageRequest.operation() != ADQueueService.OperationType.CREATE) {
				return ResponseEntity.badRequest().body("Kan kun angive nyt brugernavn for oprettelse af AD-konto");
			}

			Employee employee = employeeService.findById(adMessageRequest.employeeId());
			if (employee == null) {
				return ResponseEntity.badRequest().body("Medarbejderen findes ikke");
			}

			try {
				accountNameValidator.validate(request.newUsername());
			} catch (InvalidAccountNameException ex) {
				return ResponseEntity.badRequest().body(ex.getMessage());
			}

			if (adUserService.findByName(request.newUsername()) != null) {
				return ResponseEntity.badRequest().body("Brugernavnet er allerede i brug");
			}

			employee.setPendingAccountName(request.newUsername());
			employeeService.save(employee);

			rearmQueueItem(item);
			return ResponseEntity.ok().build();
		}).orElse(ResponseEntity.notFound().build());
	}

	private void rearmQueueItem(QueueItem item) {
		if (item.getHeaders() != null) {
			item.getHeaders().stream()
					.filter(h -> "x-attempt".equals(h.getName()))
					.forEach(h -> h.setValue("0"));
		}
		item.setStatus(QueueItem.QueueItemStatus.NEW);
		item.setDequeueTime(Instant.now());
		item.setMessageId(UUID.randomUUID().toString());
		queueItemDao.save(item);
	}

	@RequireSystemAdministrator
	@DeleteMapping("/adQueue/{id}")
	public ResponseEntity<?> deleteAdQueueItem(@PathVariable long id) {
		if (!queueItemDao.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		queueItemDao.deleteById(id);
		return ResponseEntity.ok().build();
	}

	public record InvitationRequest(String recipient, String mail) {}
	@RequireSystemAdministrator
	@PostMapping("/send/invitation")
	public ResponseEntity<?> sendInvitation(@RequestBody InvitationRequest invitationRequest) {
		if (!StringUtils.hasText(invitationRequest.recipient())) {
			return new ResponseEntity<>("Du skal angive navnet af modtageren", HttpStatus.BAD_REQUEST);
		}
		if (!StringUtils.hasText(invitationRequest.mail()) || !EMAIL_PATTERN.matcher(invitationRequest.mail()).matches()) {
			return new ResponseEntity<>("Ugyldig mailadresse", HttpStatus.BAD_REQUEST);
		}
		if (!StringUtils.hasText(configuration.getBaseUrl())) {
			return new ResponseEntity<>("Der opstod en fejl ved afsendelse af invitationen", HttpStatus.BAD_REQUEST);
		}
		EmailTemplate emailTemplate = emailTemplateService.getByType(EmailTemplateType.MAIL_INVITATION);
		if (!emailTemplate.isEnabled()) {
			return new ResponseEntity<>("Invitationsskabelonen er ikke aktiveret", HttpStatus.BAD_REQUEST);
		}

		String invitationLink = configuration.getBaseUrl() + "/landing-page";
		EmailTemplateService.RenderedEmail email = emailTemplateService.render(emailTemplate, Map.of(
				EmailTemplatePlaceholder.RECIPIENT, invitationRequest.recipient(),
				EmailTemplatePlaceholder.LINK, invitationLink));
		messageService.sendNotificationUsingMail(invitationRequest.mail(), email);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
