package dk.digitalidentity.OS2ilm.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import dk.digitalidentity.OS2ilm.model.entity.AdGroup;
import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.model.entity.dto.DeleteEmployeeLogRecord;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.service.dto.sofd.SofdPhone;
import dk.digitalidentity.OS2ilm.service.dto.sofd.SofdUser;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmployeeStatus;
import dk.digitalidentity.OS2ilm.service.dto.sofd.SofdAffiliation;
import dk.digitalidentity.OS2ilm.service.dto.sofd.SofdPerson;
import dk.digitalidentity.OS2ilm.queue.RetryableMessageHandler;
import dk.digitalidentity.simple_queue.QueueMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static dk.digitalidentity.OS2ilm.util.QueueUtils.getSofdHeaders;
import static dk.digitalidentity.OS2ilm.util.QueueUtils.maskCpr;

@Slf4j
@Service
public class SofdQueueService extends RetryableMessageHandler {
	private static final String QUEUE_IDENTIFIER = "sofdQueue";
	private static final int MAX_ATTEMPTS = 5;
	private static final String SOFD_MASTER_IDENTIFIER="OS2ILM";

	private final IlmConfiguration configuration;
	private final EmployeeService employeeService;
	private final QueueHelperService queueHelperService;
	private final RestTemplate restTemplate;
	private final AuditLogService auditLogService;

	public SofdQueueService(ApplicationEventPublisher eventPublisher,
			IlmConfiguration configuration, EmployeeService employeeService,
			QueueHelperService queueHelperService, RestTemplate restTemplate, AuditLogService auditLogService) {
		super(QUEUE_IDENTIFIER, MAX_ATTEMPTS, eventPublisher);
		this.configuration = configuration;
		this.employeeService = employeeService;
		this.queueHelperService = queueHelperService;
		this.restTemplate = restTemplate;
		this.auditLogService = auditLogService;
	}

	private void createPerson(SofdPerson person) {
		log.info("Creating new person in SOFD with CPR: {}", maskCpr(person.getCpr()));
		log.debug("Person creation request payload: firstname={}, surname={}, master={}",
				person.getFirstname(), person.getSurname(), person.getMaster());

		HttpHeaders headers = getSofdHeaders(configuration.getSofd().getApiKey());
		HttpEntity<SofdPerson> request = new HttpEntity<>(person, headers);

		String apiUrl = configuration.getSofd().getUrl() + "/api/v2/persons";
		log.debug("Sending POST request to SOFD API: {}", apiUrl);

		try {
			ResponseEntity<SofdPerson> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, SofdPerson.class);
			log.info("SOFD API response for person creation: status={}", response.getStatusCode());
			log.debug("Full SOFD API response body: {}", response.getBody());

			if (response.getStatusCode() != HttpStatus.CREATED) {
				log.error("Person creation failed with unexpected status code: {}", response.getStatusCode().value());
				throw new RuntimeException("Failed to create person in SOFD: " + response.getStatusCode().value());
			}

			log.debug("Person successfully created in SOFD, creating ADUser");
			Employee employee = employeeService.findByCprWithUserRolesAndAdGroupsAndRoleGroups(person.getCpr());
			if (employee == null) {
				log.warn("Employee for CPR {} no longer exists - skipping AD user creation", maskCpr(person.getCpr()));
				return;
			}
			if (employee.getUser() == null) {
				createUser(employee, employee.getExtUuid());
			}

			log.debug("Person successfully created in AD, updating local employee");
			employee.setIntegrated(EmployeeStatus.AWAITING_RC_INTEGRATION);
			employeeService.save(employee);
			log.info("Employee status updated to AWAITING_RC_INTEGRATION for CPR: {}", maskCpr(person.getCpr()));
		}
		catch (HttpServerErrorException e) {
			log.error("HTTP server error during person creation: status={}, response_body={}",
					e.getStatusCode(), e.getResponseBodyAsString());
			if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
				log.warn("Internal server error likely indicates CPR already exists in SOFD");
				throw new RuntimeException("Internal Server Error; Cpr number most likely exists already: " + e.getResponseBodyAsString());
			}
			throw e;
		}
	}

	private void updateExistingPerson(SofdPerson ilmPerson, SofdPerson sofdPerson, boolean markedForDeletion) {
		log.info("Updating existing person in SOFD with CPR: {}, UUID: {}",
				maskCpr(ilmPerson.getCpr()), sofdPerson.getUuid());

		log.debug("Starting person update process: updating basic info and affiliations");
		updatePerson(ilmPerson, sofdPerson);
		updateAffiliations(ilmPerson, sofdPerson, markedForDeletion);
		updatePhone(ilmPerson, sofdPerson);

		HttpHeaders headers = getSofdHeaders(configuration.getSofd().getApiKey());
		HttpEntity<SofdPerson> request = new HttpEntity<>(sofdPerson, headers);

		String apiUrl = configuration.getSofd().getUrl() + "/api/v2/persons/" + ilmPerson.getUuid();
		log.debug("Sending PATCH request to SOFD API: {}", apiUrl);

		ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.PATCH, request, String.class);
		log.info("SOFD API response for person update: status={}, body_length={}",
				response.getStatusCode(), response.getBody() != null ? response.getBody().length() : 0);
		log.debug("Full SOFD API response body: {}", response.getBody());

		if (!response.getStatusCode().is2xxSuccessful() && !response.getStatusCode().is3xxRedirection()) {
			log.error("Person update failed for UUID: {}, status_code: {}, response: {}",
					ilmPerson.getUuid(), response.getStatusCode().value(), response.getBody());
			throw new RuntimeException("Failed to update person in SOFD: " + response.getStatusCode().value());
		}

		if (markedForDeletion) {
			log.info("Successfully removed affiliation in SOFD for UUID: {}", ilmPerson.getUuid());
			return;
		}

		// Now that the user is synchronized, we need to create the AD-user for them if they don't have one yet
		Employee employee = employeeService.findByCprWithUserRolesAndAdGroupsAndRoleGroups(ilmPerson.getCpr());
		if (employee == null) {
			log.warn("Employee for CPR {} no longer exists - skipping AD user creation", maskCpr(ilmPerson.getCpr()));
			return;
		}
		if (employee.getUser() == null) {
			createUser(employee, sofdPerson.getUuid());
		}

		// Finally, the status can be set to awaiting role catalogue integration since everything else is taken care of.
		// We also update the extUuid (previously randomly generated) because we need to call sofd with it.
		if (EmployeeStatus.AWAITING_FULL_INTEGRATION.equals(employee.getIntegrated())) {
			employee.setIntegrated(EmployeeStatus.AWAITING_RC_INTEGRATION);
			employee.setExtUuid(ilmPerson.getUuid());
			employeeService.save(employee);
			log.info("Employee status updated to AWAITING_RC_INTEGRATION for CPR: {}", maskCpr(ilmPerson.getCpr()));
		}

		log.info("Person successfully updated in SOFD for UUID: {}", ilmPerson.getUuid());
	}

	private void createUser(Employee employee, String existingPerson) {
		List<Long> groupIds = new ArrayList<>();
		if (employee.getAdGroups() != null && !employee.getAdGroups().isEmpty()) {
			groupIds = employee.getAdGroups().stream().map(AdGroup::getId).toList();
		}

		Long activationId = null;
		if (employee.getActivationTimeFrame() != null) {
			activationId = employee.getActivationTimeFrame().getId();
		}

		queueHelperService.adCreateHelper(employee.getId(), activationId, groupIds, existingPerson);
	}

	private static void updateAffiliations(SofdPerson ilmPerson, SofdPerson sofdPerson, boolean markDeleted) {
		log.debug("Starting affiliation update process");

		Set<SofdAffiliation> affiliations = sofdPerson.getAffiliations();
		if (ilmPerson.getAffiliations() != null && !ilmPerson.getAffiliations().isEmpty()) {
			SofdAffiliation sofdAffiliation = ilmPerson.getAffiliations().stream().findFirst().get();
			log.debug("Processing affiliation update: masterId={}, orgUnitUuid={}, positionName={}",
					sofdAffiliation.getMasterId(), sofdAffiliation.getOrgUnitUuid(), sofdAffiliation.getPositionName());

			SofdAffiliation existingAffiliation = null;
			if (affiliations != null) {
				log.debug("Searching for existing affiliation in {} total affiliations", affiliations.size());
				for (SofdAffiliation affiliation : affiliations) {
					boolean isILMMaster = SOFD_MASTER_IDENTIFIER.equals(affiliation.getMaster());
					boolean isCorrectAffiliationId = Objects.equals(affiliation.getUuid(), sofdAffiliation.getUuid());
					if (!isILMMaster || !isCorrectAffiliationId) {
						continue;
					}
					existingAffiliation = affiliation;
					log.debug("Found existing affiliation with masterId: {}", affiliation.getMasterId());
					break;
				}
			}

			if (existingAffiliation == null) {
				log.info("Creating new affiliation for masterId: {}", sofdAffiliation.getMasterId());
				existingAffiliation = new SofdAffiliation();
				existingAffiliation.setMaster(sofdAffiliation.getMaster());
				existingAffiliation.setMasterId(sofdAffiliation.getMasterId());
				existingAffiliation.setUuid(sofdAffiliation.getUuid());
				existingAffiliation.setAffiliationType(sofdAffiliation.getAffiliationType());

				sofdPerson.getAffiliations().add(existingAffiliation);
			} else {
				log.debug("Updating existing affiliation for masterId: {}", sofdAffiliation.getMasterId());
			}

			existingAffiliation.setEmployeeId(sofdAffiliation.getEmployeeId());
			existingAffiliation.setStartDate(sofdAffiliation.getStartDate());
			existingAffiliation.setStopDate(sofdAffiliation.getStopDate());
			existingAffiliation.setPositionName(sofdAffiliation.getPositionName());
			existingAffiliation.setOrgUnitUuid(sofdAffiliation.getOrgUnitUuid());
			existingAffiliation.setInheritPrivileges(sofdAffiliation.isInheritPrivileges());

			// Only ever flips to true, and only on this one matched affiliation - a person can have several
			// affiliations from other masters (SD-LØN, OPUS, ...) that ILM must never touch.
			if (markDeleted) {
				existingAffiliation.setDeleted(true);
			}

			log.debug("Affiliation updated: startDate={}, stopDate={}, positionName={}",
					existingAffiliation.getStartDate(), existingAffiliation.getStopDate(), existingAffiliation.getPositionName());
		} else {
			log.debug("No affiliations to update in the incoming person data");
		}
	}

	private static void updatePhone(SofdPerson ilmPerson, SofdPerson sofdPerson) {
		log.debug("Starting phone update process");

		if (ilmPerson.getPhones() == null || ilmPerson.getPhones().isEmpty()) {
			log.debug("No phone in the incoming person data, nothing to update");
			return;
		}

		SofdPhone ilmPhone = ilmPerson.getPhones().stream().findFirst().get();
		if (ilmPhone.getPhoneNumber() == null || ilmPhone.getPhoneNumber().isBlank()) {
			log.debug("Incoming phone has no number, nothing to update");
			return;
		}

		// A person can have several phones in SOFD (from different masters), so we must never replace the set
		if (sofdPerson.getPhones() == null) {
			sofdPerson.setPhones(new HashSet<>());
		}
		Set<SofdPhone> phones = sofdPerson.getPhones();

		// If the number already exists in SOFD (regardless of master) there is nothing to do
		boolean numberExists = phones.stream().anyMatch(p -> ilmPhone.getPhoneNumber().trim().equals(p.getPhoneNumber() == null ? null : p.getPhoneNumber().trim()));
		if (numberExists) {
			log.debug("Phone number already exists in SOFD, skipping");
			return;
		}

		// ILM only ever owns one phone, so if SOFD has an ILM-mastered phone, that is the one that changed
		SofdPhone existingIlmPhone = phones.stream()
				.filter(p -> SOFD_MASTER_IDENTIFIER.equals(p.getMaster()))
				.findFirst()
				.orElse(null);

		if (existingIlmPhone != null) {
			log.info("Updating ILM-mastered phone number in SOFD");
			existingIlmPhone.setPhoneNumber(ilmPhone.getPhoneNumber());
			existingIlmPhone.setPhoneType(ilmPhone.getPhoneType());
		}
		else {
			log.info("Creating new phone in SOFD");
			phones.add(ilmPhone);
		}
	}

	private static void updatePerson(SofdPerson ilmPerson, SofdPerson sofdPerson) {
		log.debug("Starting person basic info update");

		if (SOFD_MASTER_IDENTIFIER.equals(sofdPerson.getMaster())) {
			log.debug("Person has OS2ILM master, updating basic information: firstname={} -> {}, surname={} -> {}",
					sofdPerson.getFirstname(), ilmPerson.getFirstname(),
					sofdPerson.getSurname(), ilmPerson.getSurname());

			sofdPerson.setFirstname(ilmPerson.getFirstname());
			sofdPerson.setSurname(ilmPerson.getSurname());
			sofdPerson.setLastChanged(ilmPerson.getLastChanged());
			sofdPerson.setChosenName(ilmPerson.getChosenName());

			log.debug("Person basic info updated successfully");
		} else {
			log.debug("Person has different master ({}), skipping basic info update", sofdPerson.getMaster());
		}
	}

	public boolean patchEmployeeIdForAdUser(AdUser adUser) {
		Employee employee = adUser.getEmployee();
		if (employee == null || employee.getCpr() == null || employee.getCpr().isBlank()) {
			log.warn("Cannot patch employeeId: missing employee or CPR for adUser {}", adUser.getName());
			return false;
		}

		SofdPerson existingPerson = fetchPerson(employee.getCpr());
		if (existingPerson == null) {
			log.info("Person not yet in SOFD for CPR: {} - retrying later", maskCpr(employee.getCpr()));
			return false;
		}

		Set<SofdUser> existingUsers = existingPerson.getUsers();
		if (existingUsers == null || existingUsers.isEmpty()) {
			log.info("SOFD has no users yet for CPR: {} - retrying later", maskCpr(employee.getCpr()));
			return false;
		}

		SofdUser targetUser = existingUsers.stream()
				.filter(u -> Objects.equals(u.getUserId(), adUser.getName()))
				.findFirst()
				.orElse(null);
		if (targetUser == null) {
			log.info("AD user {} not yet synced to SOFD - retrying later", adUser.getName());
			return false;
		}

		targetUser.setEmployeeId(employee.getExtAffiliationUuid());

		HttpHeaders headers = getSofdHeaders(configuration.getSofd().getApiKey());
		HttpEntity<SofdPerson> request = new HttpEntity<>(existingPerson, headers);

		String apiUrl = configuration.getSofd().getUrl() + "/api/v2/persons/" + existingPerson.getUuid();
		log.debug("Sending PATCH to set employeeId on user {}: {}", adUser.getName(), apiUrl);

		try {
			ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.PATCH, request, String.class);
			if (!response.getStatusCode().is2xxSuccessful() && !response.getStatusCode().is3xxRedirection()) {
				log.warn("SOFD returned non-success patching employeeId for user {}: status={}", adUser.getName(), response.getStatusCode().value());
				return false;
			}
		} catch (RestClientException e) {
			log.warn("Transient error patching employeeId for AD user {}: {}", adUser.getName(), e.getMessage());
			return false;
		}

		adUser.setExtUuid(targetUser.getUuid());

		log.info("Successfully patched employeeId on SOFD user {} for CPR: {}",
				adUser.getName(), maskCpr(employee.getCpr()));
		return true;
	}

	private SofdPerson fetchPerson(String cpr) {
		log.debug("Fetching person from SOFD by CPR: {}", maskCpr(cpr));

		HttpHeaders headers = getSofdHeaders(configuration.getSofd().getApiKey());
		HttpEntity<SofdPerson> request = new HttpEntity<>(headers);

		String apiUrl = configuration.getSofd().getUrl() + "/api/v2/persons/byCpr/" + cpr;
		log.debug("Sending GET request to SOFD API: {}", apiUrl);

		ResponseEntity<SofdPerson> response = null;
		try {
			response = restTemplate.exchange(apiUrl, HttpMethod.GET, request, SofdPerson.class);
		}
		catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				log.info("Person not found in SOFD for CPR: {}", maskCpr(cpr));
				return null;
			}
			throw new RuntimeException(e);
		}

		log.debug("SOFD API response for person fetch: status={}", response.getStatusCode());

		switch (response.getStatusCode()) {
			case HttpStatus.OK -> {
				SofdPerson fetchedPerson = response.getBody();
				log.info("Person found in SOFD: UUID={}, master={}, affiliations_count={}",
						fetchedPerson != null ? fetchedPerson.getUuid() : null,
						fetchedPerson != null ? fetchedPerson.getMaster() : null,
						fetchedPerson != null && fetchedPerson.getAffiliations() != null ? fetchedPerson.getAffiliations().size() : 0);
				return fetchedPerson;
			}
			case HttpStatus.NOT_FOUND -> {
				log.info("Person not found in SOFD for CPR: {}", maskCpr(cpr));
				return null;
			}
			default -> {
				log.error("Failed to fetch person from SOFD: status={}", response.getStatusCode().value());
				throw new RuntimeException("Failed to fetch person from SOFD: " + response.getStatusCode().value());
			}
		}
	}

	/**
	 * This is where we handle the message
	 * @param message
	 * @return Use Return true to remove message from queue, return false to keep it
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean handleMessage(QueueMessage message) {
		log.info("Processing queue message: priority={}", message.getPriority());
		log.debug("Queue message body length: {}", message.getBody() != null ? message.getBody().length() : 0);

		try {
			QueueHelperService.SofdMessageRequest sofdMessageRequest = stringToObject(message.getBody());
			log.info("Parsed queue message: person_cpr={}",
					maskCpr(sofdMessageRequest.person().getCpr()));

			boolean forDeletion = sofdMessageRequest.deleteEmployeeIdAfterSync() != null;

			SofdPerson sofdPerson = fetchPerson(sofdMessageRequest.person().getCpr());
			if (sofdPerson != null) {
				log.info("Person exists in SOFD, proceeding with update");
				sofdMessageRequest.person().setUuid(sofdPerson.getUuid());
				updateExistingPerson(sofdMessageRequest.person(), sofdPerson, forDeletion);
			}
			else {
				log.info("Person does not exist in SOFD, proceeding with creation");
				createPerson(sofdMessageRequest.person());
			}

			if (forDeletion) {
				finalizeEmployeeDeletion(sofdMessageRequest.deleteEmployeeIdAfterSync(), sofdMessageRequest.performerId(), sofdMessageRequest.performerName());
			}

			log.info("Queue message processed successfully");
			return CONSUMED;
		}
		catch (JacksonException ex) {
			log.error("JSON processing error while handling queue message: error={}",
					 ex.getMessage(), ex);
			throw new RuntimeException("Kan ikke sende opdatere person i SOFD - json processing", ex);
		}
		catch (Exception e) {
			log.error("Unexpected error while handling queue message: error={}",
					e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	// Final step of the employee deletion chain, run once the AD account is disabled and the SOFD
	// affiliation stop has been confirmed - see ADQueueService's DISABLE case and sofdHelperForDeletion.
	private void finalizeEmployeeDeletion(Long employeeId, String performerId, String performerName) {
		Employee employee = employeeService.findById(employeeId);
		if (employee == null) {
			log.warn("Employee {} not found while finalizing deletion after SOFD sync", employeeId);
			return;
		}

		auditLogService.log(performerId, performerName, employee.getId(), employee.getName(), LogAction.EMPLOYEE_DELETED, "Medarbejder blev slettet", new DeleteEmployeeLogRecord(employee.getId(), employee.getName(), employee.getEmail()));

		employeeService.delete(employee);
		log.info("Employee {} soft-deleted after AD disable and SOFD affiliation stop both succeeded", employeeId);
	}

	private QueueHelperService.SofdMessageRequest stringToObject(String body) throws JacksonException {
		ObjectMapper mapper = new ObjectMapper();
		log.debug("Converting JSON string to object: length={}", body.length());
		return mapper.readValue(body, QueueHelperService.SofdMessageRequest.class);
	}
}
