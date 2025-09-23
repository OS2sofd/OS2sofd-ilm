package dk.digitalidentity.OS2ilm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmployeeStatus;
import dk.digitalidentity.OS2ilm.service.dto.sofd.SofdAffiliation;
import dk.digitalidentity.OS2ilm.service.dto.sofd.SofdPerson;
import dk.digitalidentity.OS2ilm.service.dto.sofd.SofdUser;
import dk.digitalidentity.simple_queue.QueueMessage;
import dk.digitalidentity.simple_queue.SimpleMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SofdQueueService implements SimpleMessageHandler {
	private static final String QUEUE_IDENTIFIER = "sofdQueue";
	private static final String APPLICATION_JSON = "application/json";
	private static final String SOFD_MASTER_IDENTIFIER="OS2ILM";
	private static final String ACTIVE_DIRECTORY_USER_TYPE = "ACTIVE_DIRECTORY";
	public enum Type { CREATE, UPDATE }

	private final IlmConfiguration configuration;
	private final ApplicationEventPublisher eventPublisher;
	private final EmployeeService employeeService;

	@Autowired
	private RestTemplate restTemplate;

	private void createPerson(SofdPerson person) {
		log.info("Creating new person in SOFD with CPR: {}", maskCpr(person.getCpr()));
		log.debug("Person creation request payload: firstname={}, surname={}, master={}",
				person.getFirstname(), person.getSurname(), person.getMaster());

		HttpHeaders headers = getHeaders();
		HttpEntity<SofdPerson> request = new HttpEntity<>(person, headers);

		String apiUrl = configuration.getSofd().getUrl() + "/api/v2/persons";
		log.debug("Sending POST request to SOFD API: {}", apiUrl);

		try {
			ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);
			log.info("SOFD API response for person creation: status={}, body_length={}",
					response.getStatusCode(), response.getBody() != null ? response.getBody().length() : 0);
			log.debug("Full SOFD API response body: {}", response.getBody());

			if (response.getStatusCode() != HttpStatus.CREATED) {
				log.error("Person creation failed with unexpected status code: {}", response.getStatusCode().value());
				throw new RuntimeException("Failed to create person in SOFD: " + response.getStatusCode().value());
			}

			log.debug("Person successfully created in SOFD, updating local employee status");
			Employee employee = employeeService.findByCpr(person.getCpr());
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

	private void updateExistingPerson(SofdPerson person, SofdPerson existingPerson) {
		log.info("Updating existing person in SOFD with CPR: {}, UUID: {}",
				maskCpr(person.getCpr()), existingPerson.getUuid());

		log.debug("Starting person update process: updating basic info, users, and affiliations");
		updatePerson(person, existingPerson);
		updateUsers(person, existingPerson);
		updateAffiliations(person, existingPerson);

		HttpHeaders headers = getHeaders();
		HttpEntity<SofdPerson> request = new HttpEntity<>(existingPerson, headers);

		String apiUrl = configuration.getSofd().getUrl() + "/api/v2/persons/" + person.getUuid();
		log.debug("Sending PATCH request to SOFD API: {}", apiUrl);

		ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.PATCH, request, String.class);
		log.info("SOFD API response for person update: status={}, body_length={}",
				response.getStatusCode(), response.getBody() != null ? response.getBody().length() : 0);
		log.debug("Full SOFD API response body: {}", response.getBody());

		if (!response.getStatusCode().is2xxSuccessful() && !response.getStatusCode().is3xxRedirection()) {
			log.error("Person update failed for UUID: {}, status_code: {}, response: {}",
					person.getUuid(), response.getStatusCode().value(), response.getBody());
			throw new RuntimeException("Failed to update person in SOFD: " + response.getStatusCode().value());
		}

		Employee employee = employeeService.findByCpr(person.getCpr());
		if (EmployeeStatus.AWAITING_FULL_INTEGRATION.equals(employee.getIntegrated())) {
			employee.setIntegrated(EmployeeStatus.AWAITING_RC_INTEGRATION);
			employeeService.save(employee);
			log.info("Employee status updated to AWAITING_RC_INTEGRATION for CPR: {}", maskCpr(person.getCpr()));
		}

		log.info("Person successfully updated in SOFD for UUID: {}", person.getUuid());
	}

	private static void updateAffiliations(SofdPerson person, SofdPerson existingPerson) {
		log.debug("Starting affiliation update process");

		Set<SofdAffiliation> affiliations = existingPerson.getAffiliations();
		if (person.getAffiliations() != null && !person.getAffiliations().isEmpty()) {
			SofdAffiliation sofdAffiliation = person.getAffiliations().stream().findFirst().get();
			log.debug("Processing affiliation update: masterId={}, orgUnitUuid={}, positionName={}",
					sofdAffiliation.getMasterId(), sofdAffiliation.getOrgUnitUuid(), sofdAffiliation.getPositionName());

			SofdAffiliation existingAffiliation = null;
			if (affiliations != null) {
				log.debug("Searching for existing affiliation in {} total affiliations", affiliations.size());
				for (SofdAffiliation affiliation : affiliations) {
					if (!SOFD_MASTER_IDENTIFIER.equals(affiliation.getMaster()) ||
							!Objects.equals(affiliation.getMasterId(), sofdAffiliation.getMasterId())) {
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

				existingPerson.getAffiliations().add(existingAffiliation);
			} else {
				log.debug("Updating existing affiliation for masterId: {}", sofdAffiliation.getMasterId());
			}

			existingAffiliation.setStartDate(sofdAffiliation.getStartDate());
			existingAffiliation.setStopDate(sofdAffiliation.getStopDate());
			existingAffiliation.setPositionName(sofdAffiliation.getPositionName());
			existingAffiliation.setOrgUnitUuid(sofdAffiliation.getOrgUnitUuid());

			log.debug("Affiliation updated: startDate={}, stopDate={}, positionName={}",
					existingAffiliation.getStartDate(), existingAffiliation.getStopDate(), existingAffiliation.getPositionName());
		} else {
			log.debug("No affiliations to update in the incoming person data");
		}
	}

	private static void updateUsers(SofdPerson ilmPerson, SofdPerson sofdPerson) {
		log.debug("Starting user update process");

		Set<SofdUser> sofdUsers = sofdPerson.getUsers();
		if (ilmPerson.getUsers() != null && !ilmPerson.getUsers().isEmpty()) {
			SofdUser ilmUser = ilmPerson.getUsers().stream().findFirst().get();
			log.debug("Processing user update: masterId={}, userId={}, userType={}",
					ilmUser.getMasterId(), ilmUser.getUserId(), ilmUser.getUserType());

			SofdUser existingUser = null;
			if (sofdUsers != null) {
				log.debug("Searching for existing user in {} total users", sofdUsers.size());
				for (SofdUser sofdUser : sofdUsers) {
					if (!SOFD_MASTER_IDENTIFIER.equals(sofdUser.getMaster()) ||
							!ACTIVE_DIRECTORY_USER_TYPE.equals(sofdUser.getUserType()) ||
							!Objects.equals(sofdUser.getMasterId(), ilmUser.getMasterId())) {
						continue;
					}
					existingUser = sofdUser;
					log.debug("Found existing user with masterId: {}", sofdUser.getMasterId());
					break;
				}
			}

			if (existingUser == null) {
				log.info("Creating new user for masterId: {}, userId: {}", ilmUser.getMasterId(), ilmUser.getUserId());
				existingUser = new SofdUser();
				existingUser.setUserType(ilmUser.getUserType());
				existingUser.setUuid(ilmUser.getUuid());
				existingUser.setMaster(ilmUser.getMaster());
				existingUser.setMasterId(ilmUser.getMasterId());
				existingUser.setUserId(ilmUser.getUserId());
				sofdPerson.getUsers().add(existingUser);
			} else {
				log.debug("User already exists for masterId: {}, no additional updates needed", ilmUser.getMasterId());
			}
		} else {
			log.debug("No users to update in the incoming person data");
		}
	}

	private static void updatePerson(SofdPerson person, SofdPerson existingPerson) {
		log.debug("Starting person basic info update");

		if (SOFD_MASTER_IDENTIFIER.equals(existingPerson.getMaster())) {
			log.debug("Person has OS2ILM master, updating basic information: firstname={} -> {}, surname={} -> {}",
					existingPerson.getFirstname(), person.getFirstname(),
					existingPerson.getSurname(), person.getSurname());

			existingPerson.setFirstname(person.getFirstname());
			existingPerson.setSurname(person.getSurname());
			existingPerson.setLastChanged(person.getLastChanged());
			existingPerson.setChosenName(person.getChosenName());

			log.debug("Person basic info updated successfully");
		} else {
			log.debug("Person has different master ({}), skipping basic info update", existingPerson.getMaster());
		}
	}

	private SofdPerson fetchPerson(String cpr) {
		log.debug("Fetching person from SOFD by CPR: {}", maskCpr(cpr));

		HttpHeaders headers = getHeaders();
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
				log.info("Person found in SOFD: UUID={}, master={}, affiliations_count={}, users_count={}",
						fetchedPerson != null ? fetchedPerson.getUuid() : null,
						fetchedPerson != null ? fetchedPerson.getMaster() : null,
						fetchedPerson != null && fetchedPerson.getAffiliations() != null ? fetchedPerson.getAffiliations().size() : 0,
						fetchedPerson != null && fetchedPerson.getUsers() != null ? fetchedPerson.getUsers().size() : 0);
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

	private HttpHeaders getHeaders() {
		log.debug("Creating HTTP headers for SOFD API request");
		HttpHeaders headers = new HttpHeaders();
		headers.add("ApiKey", configuration.getSofd().getApiKey());
		headers.add("Content-Type", APPLICATION_JSON);
		headers.add("Accept", APPLICATION_JSON);

		return headers;
	}

	/**
	 * Simple queue uses this to check if we can handle the message
	 * @param message
	 * @return
	 */
	@Override
	public boolean handles(QueueMessage message) {
		boolean canHandle = QUEUE_IDENTIFIER.equals(message.getQueue());
		log.debug("Queue message handler check: queue={}, can_handle={}", message.getQueue(), canHandle);
		return canHandle;
	}

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
			SofdMessageRequest sofdMessageRequest = stringToObject(message.getBody());
			log.info("Parsed queue message: person_cpr={}",
					maskCpr(sofdMessageRequest.person.getCpr()));

			SofdPerson existingPerson = fetchPerson(sofdMessageRequest.person.getCpr());
			if (existingPerson != null) {
				log.info("Person exists in SOFD, proceeding with update");
				sofdMessageRequest.person.setUuid(existingPerson.getUuid());
				updateExistingPerson(sofdMessageRequest.person, existingPerson);
			}
			else {
				log.info("Person does not exist in SOFD, proceeding with creation");
				createPerson(sofdMessageRequest.person);
			}

			log.info("Queue message processed successfully");
			return true;
		}
		catch (JsonProcessingException ex) {
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

	/**
	 * Any failed message from this queue lands here
	 * @param message
	 * @param exception
	 * @return Use return true to indicate it has been handled, return false if message should be placed in the DEADLETTER queue
	 */
	@Override
	public boolean handleFailedMessage(QueueMessage message, Exception exception) {
		log.error("Queue message failed processing: queue={}, priority={}, error={}",
				message.getQueue(), message.getPriority(), exception.getMessage(), exception);
		log.debug("Failed message body: {}", message.getBody());
		return false;
	}

	public record SofdMessageRequest(SofdPerson person) { }

	public boolean addMessageToQueue(SofdMessageRequest message) throws NullPointerException, IllegalArgumentException {
		log.info("Adding message to SOFD queue: person_cpr={}",
				maskCpr(message.person.getCpr()));
		log.debug("Queue message details: firstname={}, surname={}, master={}",
				message.person.getFirstname(), message.person.getSurname(), message.person.getMaster());

		validateMessage(message);

		try {
			QueueMessage queueMessage = QueueMessage.builder()
					.queue(QUEUE_IDENTIFIER)
					.priority(1L)
					.body(objectToJson(message))
					.build();

			log.debug("Publishing queue message event: queue={}, priority={}, body_length={}",
					QUEUE_IDENTIFIER, queueMessage.getPriority(), queueMessage.getBody().length());

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
		if (!StringUtils.hasText(person.getCpr()) ||
				!StringUtils.hasText(person.getMaster()) ||
				!StringUtils.hasText(person.getFirstname()) ||
				!StringUtils.hasText(person.getSurname())
		) {
			log.error("Invalid person object: cpr_present={}, master_present={}, firstname_present={}, surname_present={}",
					StringUtils.hasText(person.getCpr()),
					StringUtils.hasText(person.getMaster()),
					StringUtils.hasText(person.getFirstname()),
					StringUtils.hasText(person.getSurname()));
			throw new IllegalArgumentException("Invalid person object");
		}

		log.debug("Message validation completed successfully");
	}

	private String objectToJson(Object object) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
		log.debug("Converted object to JSON: length={}", json.length());
		return json;
	}

	private SofdMessageRequest stringToObject(String body) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		log.debug("Converting JSON string to object: length={}", body.length());
		return mapper.readValue(body, SofdMessageRequest.class);
	}

	/**
	 * Masks CPR number for logging purposes while preserving some identifiable information
	 */
	private String maskCpr(String cpr) {
		return "REDACTED_CPR";
	}
}
