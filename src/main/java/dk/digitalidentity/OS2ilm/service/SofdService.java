package dk.digitalidentity.OS2ilm.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import dk.digitalidentity.OS2ilm.model.entity.dto.CreateAccountOrderDTO;
import dk.digitalidentity.OS2ilm.model.entity.dto.NamesDTO;
import dk.digitalidentity.OS2ilm.model.entity.enums.AccountOrderResult;
import dk.digitalidentity.OS2ilm.service.dto.sofd.SofdPhone;
import dk.digitalidentity.OS2ilm.util.NameUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.controller.api.model.SimpleOrgUnit;
import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.OrgUnit;
import dk.digitalidentity.OS2ilm.model.entity.enums.AffiliationType;
import dk.digitalidentity.OS2ilm.model.entity.enums.ExchangeAccountStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.SofdSyncStatus;
import dk.digitalidentity.OS2ilm.model.exception.SofdIntegrationException;
import dk.digitalidentity.OS2ilm.service.dto.sofd.SofdAffiliation;
import dk.digitalidentity.OS2ilm.service.dto.sofd.SofdPerson;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static dk.digitalidentity.OS2ilm.util.QueueUtils.getSofdHeaders;

@Slf4j
@Service
@RequiredArgsConstructor
public class SofdService {
	private static final String SOFD_MASTER_IDENTIFIER="OS2ILM";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final String USER_TYPE = "EXCHANGE";

	private final OrgUnitService orgUnitService;
	private final AdUserService adUserService;
	private final EmployeeService employeeService;
	private final IlmConfiguration configuration;
	private final RestTemplate restTemplate;

	public record OrgUnitResult(Set<SimpleOrgUnit> orgUnits, long page) {}
	@Transactional(rollbackFor = Exception.class)
	public void syncData() {
		try {
			OrgUnitResult data = fetchDataFromSofd();

			List<String> existingUuids = orgUnitService.findAllUuids();
			List<String> newUuids = data.orgUnits().stream()
					.filter(ou -> ou.getDeleted() != null && !ou.getDeleted())
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

	private OrgUnitResult fetchDataFromSofd() throws Exception {
		HttpHeaders headers = getSofdHeaders(configuration.getSofd().getApiKey());

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

	public AccountOrderResult createExchangeAccount(String personUuid, String userId, String affiliationUuid) {
		if (!StringUtils.hasText(personUuid) || !StringUtils.hasText(userId) || !StringUtils.hasText(affiliationUuid)) {
			log.error("Cannot order Exchange account, missing required field(s): personUuid={}, userId={}, affiliationUuid={}",
					personUuid, userId, affiliationUuid);
			return AccountOrderResult.PERMANENT_FAILURE;
		}

		CreateAccountOrderDTO order = CreateAccountOrderDTO.builder()
				.personUuid(personUuid)
				.userType(USER_TYPE)
				.userId(userId)
				.affiliationUuid(affiliationUuid)
				.build();

		HttpHeaders headers = getSofdHeaders(configuration.getSofd().getApiKey());
		HttpEntity<CreateAccountOrderDTO> request = new HttpEntity<>(order, headers);
		String url = configuration.getSofd().getUrl() + "/api/account";

		try {
			restTemplate.exchange(url, HttpMethod.POST, request, String.class);
			log.info("Successfully ordered an Exchange Account for userId={}", userId);
			return AccountOrderResult.ORDERED;
		}
		catch (HttpClientErrorException.NotFound ex) {
			log.error("The person or userType does not exist in SOFD: personUuid={}, userType={}", personUuid, USER_TYPE);
			return AccountOrderResult.PERMANENT_FAILURE;
		}
		catch (HttpClientErrorException.BadRequest ex) {
			log.error("The chosen userId is not valid for ordering an Exchange Account: userId={}", userId);
			return AccountOrderResult.PERMANENT_FAILURE;
		}
		catch (RestClientException e) {
			// SOFD only returns 404/400/200 on this endpoint, so anything reaching here is a
			// 5xx or connectivity/timeout failure - safe to retry
			log.warn("Transient error ordering Exchange account for userId={}: {}", userId, e.getMessage());
			return AccountOrderResult.TRANSIENT_FAILURE;
		}
	}

	@Transactional
	public void applyAccountOrderResult(Employee employee, AdUser adUser, AccountOrderResult result) {
		switch (result) {
			case ORDERED -> {
				adUser.setSofdSyncStatus(SofdSyncStatus.EXCHANGE_REQUESTED);
				adUserService.save(adUser);
				employee.setExchangeAccountStatus(ExchangeAccountStatus.HAS_ACCOUNT);
				employeeService.save(employee);
			}
			case PERMANENT_FAILURE -> {
				adUser.setSofdSyncStatus(SofdSyncStatus.EXCHANGE_FAILED);
				adUserService.save(adUser);
				employee.setExchangeAccountStatus(ExchangeAccountStatus.ORDER_FAILED);
				employeeService.save(employee);
			}
			case TRANSIENT_FAILURE -> { /* no state change — will be retried */ }
		}
	}

	// Format local date to yyyy-MM-dd
	public static String formatDate(LocalDate date) {
		if (date != null) {
			return date.format(DATE_FORMATTER);
		}
		return null;
	}

	// Format local datetime to yyyy-MM-dd HH:mm:ss
	public static String formatDateTime(LocalDateTime dateTime) {
		return dateTime.format(DATETIME_FORMATTER);
	}

	public SofdPerson createSofdPerson(Employee employee) {
		if (!StringUtils.hasText(employee.getCpr())) {
			throw new IllegalArgumentException("Cpr must be filled out");
		}

		LocalDateTime lastChanged = LocalDateTime.now();
		NamesDTO extractedName = NameUtils.splitName(employee);

		return getSofdPerson(employee, extractedName, lastChanged);
	}

	private SofdPerson getSofdPerson(Employee employee, NamesDTO extractedName, LocalDateTime lastChanged) {
		HashSet<SofdAffiliation> affiliations = new HashSet<>();
		if (employee.getOrgUnit() != null && employee.getBusiness() != null) {
			affiliations.add(
					SofdAffiliation.builder()
							.master(SOFD_MASTER_IDENTIFIER)
							.masterId(employee.getExtUuid())
							.uuid(employee.getExtAffiliationUuid())
							.employeeId(employee.getExtAffiliationUuid())
							.startDate(formatDate(employee.getStartDate()))
							.stopDate(formatDate(employee.getStopDate()))
							.positionName(employee.getPositionName())
							.positionShort(employee.getPositionName())
							.affiliationType(AffiliationType.EXTERNAL.name())
							.orgUnitUuid(employee.getOrgUnit().getUuid())
							.inheritPrivileges(employee.getBusiness().isInheritPrivileges())
							.build()
			);
		}

		HashSet<SofdPhone> phones = new HashSet<>();
		if (StringUtils.hasText(employee.getPhone())) {
			phones.add(
					SofdPhone.builder()
							.master(SOFD_MASTER_IDENTIFIER)
							.masterId(employee.getExtUuid())
							.phoneNumber(employee.getPhone())
							.phoneType("MOBILE")
							.build()
			);
		}

		return SofdPerson.builder()
				.cpr(employee.getCpr())
				.master(SOFD_MASTER_IDENTIFIER)
				.firstname(extractedName.firstName())
				.surname(extractedName.lastName())
				.uuid(employee.getExtUuid())
				.lastChanged(formatDateTime(lastChanged))
				.chosenName(employee.getName())
				.affiliations(affiliations)
				.phones(phones)
				.build();
	}
}
