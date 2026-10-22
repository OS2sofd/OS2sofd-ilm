package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.exception.SofdValidationException;
import dk.digitalidentity.OS2ilm.service.dto.sofd.SofdPerson;
import dk.digitalidentity.OS2ilm.util.RandomPasswordGeneratorUtil;
import dk.digitalidentity.simple_queue.QueueMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.core.JacksonException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static dk.digitalidentity.OS2ilm.queue.QueueConstants.AD_QUEUE;
import static dk.digitalidentity.OS2ilm.queue.QueueConstants.SOFD_QUEUE;
import static dk.digitalidentity.OS2ilm.util.QueueUtils.maskCpr;
import static dk.digitalidentity.OS2ilm.util.QueueUtils.objectToJson;
import static dk.digitalidentity.OS2ilm.util.QueueUtils.getSofdHeaders;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueHelperService {
	private static final String SOFD_QUEUE_IDENTIFIER = SOFD_QUEUE;
	private static final String AD_QUEUE_IDENTIFIER = AD_QUEUE;

	private final EmployeeService employeeService;
	private final SofdService sofdService;
	private final ApplicationEventPublisher eventPublisher;
	private final IlmConfiguration configuration;

	private final RestTemplate restTemplate;
	private final EncryptionUtil encryptionUtil;

	@Transactional(rollbackFor = Exception.class)
	public void sofdHelper(Long employeeId) {
		Employee employee = employeeService.findById(employeeId);
		if (employee == null) {
			log.warn("Employee with id {} not found", employeeId);
			throw new RuntimeException("Employee with id " + employeeId + " not found");
		}

		SofdPerson sofdPerson = sofdService.createSofdPerson(employee);
		addMessageToQueue(new SofdMessageRequest(sofdPerson, null, null, null));
	}

	@Transactional(rollbackFor = Exception.class)
	public void sofdHelperForDeletion(Long employeeId, String performerId, String performerName) {
		Employee employee = employeeService.findById(employeeId);
		if (employee == null) {
			log.warn("Employee with id {} not found", employeeId);
			throw new RuntimeException("Employee with id " + employeeId + " not found");
		}

		SofdPerson sofdPerson = sofdService.createSofdPerson(employee);
		addMessageToQueue(new SofdMessageRequest(sofdPerson, employeeId, performerId, performerName));
	}

	@Transactional(rollbackFor = Exception.class)
	public String sofdGenerationHelper(Long employeeId, String personUuid) {
		return getSofdGeneratedUsername(employeeId, personUuid);
	}

	@Transactional(rollbackFor = Exception.class)
	public void adCreateHelper(Long employeeId, Long activationTimeFrameId, List<Long> groupIds, String personUuid) {
		addCreateUserToQueue(employeeId, activationTimeFrameId, groupIds, personUuid);
	}

	public record SofdMessageRequest(SofdPerson person, Long deleteEmployeeIdAfterSync, String performerId, String performerName) { }
	public boolean addMessageToQueue(SofdMessageRequest message) throws NullPointerException, SofdValidationException {
		log.info("Adding message to SOFD queue: person_cpr={}",
				maskCpr(message.person.getCpr()));
		log.debug("Queue message details: firstname={}, surname={}, master={}",
				message.person.getFirstname(), message.person.getSurname(), message.person.getMaster());

		validateMessage(message);

		try {
			QueueMessage queueMessage = QueueMessage.builder()
					.queue(SOFD_QUEUE_IDENTIFIER)
					.priority(1L)
					.body(objectToJson(message))
					.build();

			log.debug("Publishing queue message event: queue={}, priority={}, body_length={}",
					SOFD_QUEUE_IDENTIFIER, queueMessage.getPriority(), queueMessage.getBody().length());

			eventPublisher.publishEvent(queueMessage);
			log.info("Message successfully added to SOFD queue: person_cpr={}", maskCpr(message.person.getCpr()));
		}
		catch (Exception ex) {
			log.error("Failed to add message to queue: person_cpr={}, error={}",
					maskCpr(message.person.getCpr()), ex.getMessage(), ex);
			return false;
		}

		return true;
	}

	private static void validateMessage(SofdMessageRequest message) {
		log.debug("Validating SOFD queue message");

		Objects.requireNonNull(message);
		Objects.requireNonNull(message.person);

		SofdPerson person = message.person;
		List<String> missingFields = new ArrayList<>();
		if (!StringUtils.hasText(person.getCpr())) {
			missingFields.add("CPR-nummer");
		}
		if (!StringUtils.hasText(person.getMaster())) {
			missingFields.add("master");
		}
		if (!StringUtils.hasText(person.getFirstname())) {
			missingFields.add("fornavn");
		}
		if (!StringUtils.hasText(person.getSurname())) {
			missingFields.add("efternavn");
		}

		if (!missingFields.isEmpty()) {
			log.error("Invalid person object: cpr_present={}, master_present={}, firstname_present={}, surname_present={}",
					StringUtils.hasText(person.getCpr()),
					StringUtils.hasText(person.getMaster()),
					StringUtils.hasText(person.getFirstname()),
					StringUtils.hasText(person.getSurname()));
			throw new SofdValidationException("Medarbejderen kan ikke sendes til SOFD, da følgende mangler: " + String.join(", ", missingFields));
		}

		log.debug("Message validation completed successfully");
	}

	public boolean addCreateUserToQueue(Long employeeId, Long activationTimeFrameId, List<Long> groupIds, String  personUuid) {
		try {
			String password = RandomPasswordGeneratorUtil.generateUserFriendlyPassword(
					configuration.getActiveDirectoryConfig().getPasswordLength());
			ADQueueService.ADMessageRequest message = ADQueueService.ADMessageRequest.builder()
					.operation(ADQueueService.OperationType.CREATE)
					.employeeId(employeeId)
					.activationTimeFrameId(activationTimeFrameId)
					.groupIds(groupIds) // groupIds for post-creation
					.personUuid(personUuid) // uuid of person from Sofd (in case they already exist)
					.password(encryptionUtil.encryptPassword(password)) // generated once here so retries use the same password
					.build();
			return publishToQueue(message);
		} catch (Exception ex) {
			log.error("Failed to add CREATE user message to queue: employeeId={}, groupCount={}",
					employeeId, groupIds != null ? groupIds.size() : 0, ex);
			return false;
		}
	}

	private boolean publishToQueue(ADQueueService.ADMessageRequest message) throws JacksonException {
		QueueMessage queueMessage = QueueMessage.builder()
				.queue(AD_QUEUE_IDENTIFIER)
				.priority(1L)
				.body(objectToJson(message))
				.build();

		log.debug("Publishing queue message event: queue={}, operation={}, employeeId={}",
				AD_QUEUE_IDENTIFIER, message.operation(), message.employeeId());

		eventPublisher.publishEvent(queueMessage);
		return true;
	}

	public String getSofdGeneratedUsername(Long employeeId, String personUuid) {
		Employee employee = employeeService.findById(employeeId);
		if (employee == null) {
			log.warn("Employee with id {} not found", employeeId);
			throw new RuntimeException("Employee with id " + employeeId + " not found");
		}

		log.debug("Retrieving username for person from SOFD by UUID: {}", personUuid);

		HttpHeaders headers = getSofdHeaders(configuration.getSofd().getApiKey());
		HttpEntity<SofdPerson> request = new HttpEntity<>(headers);

		String apiUrl = configuration.getSofd().getUrl() + "/api/account/generateUsername?personUuid=" + personUuid + "&userType=ACTIVE_DIRECTORY&forceNewUsername=true";
		log.debug("Sending GET request to SOFD API: {}", apiUrl);

		try {
			ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, request, String.class);

			if (response.getStatusCode() != HttpStatus.OK) {
				log.error("Failed to fetch username from SOFD: status={}", response.getStatusCode().value());
				throw new RuntimeException("Failed to fetch username from SOFD: " + response.getStatusCode().value());
			}

			String username = response.getBody();
			log.debug("SOFD API response for username fetch: status={}, body_length={}", response.getStatusCode(), username != null ? username.length() : 0);
			log.debug("SOFD generated username: {}", username);

			if (!StringUtils.hasText(username)) {
				log.error("SOFD returned empty username for employee {}", employeeId);
				throw new RuntimeException("SOFD returned empty username for employee " + employeeId);
			}

			return username;
		}
		catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
				log.info("Person not found in SOFD for UUID: {}", personUuid);
			}
			throw new RuntimeException(e);
		}
	}
}
