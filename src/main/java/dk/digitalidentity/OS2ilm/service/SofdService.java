package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.controller.api.model.SimpleOrgUnit;
import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.OrgUnit;
import dk.digitalidentity.OS2ilm.model.exception.SofdIntegrationException;
import dk.digitalidentity.OS2ilm.model.entity.enums.AffiliationType;
import dk.digitalidentity.OS2ilm.service.dto.sofd.SofdAffiliation;
import dk.digitalidentity.OS2ilm.service.dto.sofd.SofdPerson;
import dk.digitalidentity.OS2ilm.service.dto.sofd.SofdUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SofdService {
	private static final String APPLICATION_JSON = "application/json";
	private static final String SOFD_MASTER_IDENTIFIER="OS2ILM";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


	private final OrgUnitService orgUnitService;
	private final IlmConfiguration configuration;
	private final RestTemplate restTemplate;
	private final EmployeeService employeeService;
	private final SofdQueueService sofdQueueService;

	public record OrgUnitResult(Set<SimpleOrgUnit> orgUnits, long page) {}
	@Transactional(rollbackOn = Exception.class)
	public void syncData() {
		try {
			OrgUnitResult data = fetchDataFromSofd();

			List<String> existingUuids = orgUnitService.findAllUuids();
			List<String> newUuids = data.orgUnits().stream()
					.map(this::persist)
					.filter(Objects::nonNull)
					.map(OrgUnit::getUuid)
					.toList();

			List<String> uuidsToDelete = existingUuids.stream()
					.filter(uuid -> !newUuids.contains(uuid))
					.toList();

			if (!uuidsToDelete.isEmpty()) {
				log.debug("Deleting UUIDs that no longer exist in Sofd " + uuidsToDelete.size());
				orgUnitService.deleteByUuidIn(uuidsToDelete);
			}
		}
		catch (Exception ex) {
			log.error("Error occurred while fetching data from Sofd.", ex);
		}
	}

	private OrgUnit persist(SimpleOrgUnit simpleOrgUnit) {
		if (simpleOrgUnit.getUuid() == null) {
			log.warn("missing uuid on OU " + simpleOrgUnit);
			return null;
		}
		OrgUnit ou = orgUnitService.findOrgUnitByUuid(simpleOrgUnit.getUuid());
		if (ou == null) {
			// Create
			createOrgUnit(simpleOrgUnit);
		}
		else {
			// Update
			updateOrgUnit(ou, simpleOrgUnit);
		}

		return ou;
	}

	private void createOrgUnit(SimpleOrgUnit simpleOrgUnit) {
		OrgUnit ou = new OrgUnit();
		ou.setUuid(simpleOrgUnit.getUuid());
		ou.setName(simpleOrgUnit.getName());
		ou.setParentUuid(simpleOrgUnit.getParentUuid());
		orgUnitService.save(ou);
	}

	private void updateOrgUnit(OrgUnit ou, SimpleOrgUnit simpleOrgUnit) {
		ou.setUuid(simpleOrgUnit.getUuid());
		ou.setName(simpleOrgUnit.getName());
		ou.setParentUuid(simpleOrgUnit.getParentUuid());
		orgUnitService.save(ou);
	}

	private HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("ApiKey", configuration.getSofd().getApiKey());
		headers.add("Content-Type", APPLICATION_JSON);
		headers.add("Accept", APPLICATION_JSON);

		return headers;
	}

	public record CreateAffiliationApiDTO(String cpr, String firstname, String surname, String orgUnitUuid, Date startDate, Date stopDate, String positionName, AffiliationType affiliationType) {}
	public boolean createAffiliationInSofd(CreateAffiliationApiDTO dto) {
		log.debug("creating affiliation in Sofd");

		HttpHeaders headers = getHeaders();

		HttpEntity<CreateAffiliationApiDTO> request = new HttpEntity<>(dto, headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(configuration.getSofd().getUrl() + "/api/createAffiliation", HttpMethod.POST, request, String.class);
			if (response.getStatusCode() == HttpStatus.OK) {
				return true;
			}
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				log.warn(e.getResponseBodyAsString());
			}
		}  catch (HttpServerErrorException e) {
			if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
				log.warn("Internal Server Error; Cpr number most likely exists already: {}", e.getResponseBodyAsString());
			}
		} catch (Exception e) {
			log.error("Unexpected error occurred while creating affiliation in Sofd: {}", e.getMessage(), e);
		}
		return false;
	}

	private OrgUnitResult fetchDataFromSofd() throws Exception {
		HttpHeaders headers = getHeaders();

		HttpEntity<List<OrgUnitResult>> request = new HttpEntity<>(headers);

		// TODO: paginate, do not fetch 5000 at a time
		ResponseEntity<OrgUnitResult> response = restTemplate.exchange(
				configuration.getSofd().getUrl() + "/api/v2/orgUnits?size=5000", HttpMethod.GET, request,
				new ParameterizedTypeReference<OrgUnitResult>() {
				});

		if (response.getStatusCode() != HttpStatus.OK) {
			throw new SofdIntegrationException("Error connecting to Sofd: " + configuration.getSofd().getUrl() + " StatusCode: " + response.getStatusCode());
		}

		return response.getBody();
	}

	// Format local date to yyyy-MM-dd
	public static String formatDate(LocalDate date) {
		return date.format(DATE_FORMATTER);
	}

	// Format local datetime to yyyy-MM-dd HH:mm:ss
	public static String formatDateTime(LocalDateTime dateTime) {
		return dateTime.format(DATETIME_FORMATTER);
	}

	public SofdPerson createSofdPerson(Employee consultant) {
		// We should only create people in Sofd that have a Cpr number
		if (!StringUtils.hasText(consultant.getCpr())) {
			throw new IllegalArgumentException("Cpr must be filled out");
		}

		LocalDateTime lastChanged = LocalDateTime.now();
		Names extractedName = extractNames(consultant);

		HashSet<SofdUser> users = new HashSet<>();
		AdUser user = consultant.getUser();
		if (user != null) {
			users.add(new SofdUser(
					SOFD_MASTER_IDENTIFIER,
					user.getExtUuid(),
					user.getExtUuid(),
					user.getName(),
					"ACTIVE_DIRECTORY"));
		}

		HashSet<SofdAffiliation> affiliations = new HashSet<>();
		if (consultant.getOrgUnit() != null) {
			affiliations.add(new SofdAffiliation(
					SOFD_MASTER_IDENTIFIER,
					consultant.getExtUuid(),
					consultant.getExtUuid(),
					formatDate(consultant.getStartDate()),
					formatDate(consultant.getStopDate()),
					consultant.getPositionName(),
					AffiliationType.EXTERNAL.name(),
					consultant.getOrgUnit().getUuid()
			));
		}

		return new SofdPerson(
				consultant.getCpr(),
				SOFD_MASTER_IDENTIFIER,
				extractedName.firstname(),
				extractedName.surname(),
				consultant.getExtUuid(),
				formatDateTime(lastChanged),
				consultant.getName(),
				users,
				affiliations
		);
	}

	private static @NotNull Names extractNames(Employee consultant) {
		String[] parts = {};
		String firstname = "";
		String surname = "";

		String name = consultant.getName();
		if (name != null && !name.trim().isEmpty()) {
			parts = name.trim().split("\\s+");
			if (parts.length > 0) {
				surname = parts[parts.length - 1];
				if (parts.length > 1) {
					firstname = String.join(" ", Arrays.copyOfRange(parts, 0, parts.length - 1));
				}
			}
		}
		return new Names(firstname, surname);
	}

	private record Names(String firstname, String surname) {
	}

	public void sofdCreateAndUpdateHelper(Employee consultant) {
		SofdPerson sofdPerson = createSofdPerson(consultant);
		sofdQueueService.addMessageToQueue(new SofdQueueService.SofdMessageRequest(sofdPerson));
	}
}
