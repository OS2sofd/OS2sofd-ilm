package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.ItSystem;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmployeeStatus;
import dk.digitalidentity.OS2ilm.model.exception.RoleCatalogIntegrationException;
import dk.digitalidentity.OS2ilm.model.entity.UserRole;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleCatalogueService {
	private static final String APPLICATION_JSON = "application/json";

	private final IlmConfiguration configuration;
	private final ItSystemService itSystemService;
	private final EmployeeService employeeService;

	public record UserRoleNewApiDTO(long id, String name, String description) {}
	public record NewApiDTO(long itSystemId, String itSystemName, List<UserRoleNewApiDTO> roles) {}

	public boolean assignUserRoleToUser(long userRoleId, String userUuid) {
		log.debug("Assigning " + userRoleId + " to " + userUuid);

		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = getHeaders();

		HttpEntity<String> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(configuration.getRc().getUrl() + "/api/user/" + userUuid + "/assign/userrole/" + userRoleId, HttpMethod.PUT, request, String.class);
			if (response.getStatusCode() == HttpStatus.OK) {
				return true;
			}
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				log.warn(e.getResponseBodyAsString());
			}
		}
		return false;
	}

	public boolean unassignUserRoleToUser(long userRoleId, String userUuid) {
		log.debug("Unassigning " + userRoleId + " to " + userUuid);

		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = getHeaders();

		HttpEntity<String> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(configuration.getRc().getUrl() + "/api/user/" + userUuid + "/deassign/userrole/" + userRoleId, HttpMethod.DELETE, request, String.class);
			if (response.getStatusCode() == HttpStatus.OK) {
				return true;
			}
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				log.warn(e.getResponseBodyAsString());
				return true;
			}
		}
		return false;
	}

	@Transactional(rollbackOn = Exception.class)
	public void syncData() {
		int updated = 0, created = 0, deleted = 0;

		try {
			List<NewApiDTO> data = fetchDataFromRC();
			List<ItSystem> itSystems = itSystemService.findAll();

			if (data != null) {
				for (NewApiDTO newApiDTO : data) {
					long itSystemId = newApiDTO.itSystemId();
					String itSystemName = newApiDTO.itSystemName();
					List<UserRoleNewApiDTO> roles = newApiDTO.roles();

					Optional<ItSystem> existingItSystem = itSystems.stream().filter(its -> its.getItSystemId() == itSystemId).findAny();
					ItSystem itSystem = null;
					if (existingItSystem.isPresent()) {
						//update
						itSystem = existingItSystem.get();
						boolean hasChanges = false;
						boolean userRoleChanges = updateRoles(itSystem, roles);

						if (!Objects.equals(itSystem.getName(), itSystemName)) {
							hasChanges = true;
						}

						if (userRoleChanges || hasChanges) {
							log.debug("Updating ItSystem: " + itSystem.getName());
							itSystem.setName(itSystemName);
							updated++;
						}
					}
					else {
						//create
						log.debug("Creating ItSystem: " + itSystemName);
						itSystem = new ItSystem();
						itSystem.setItSystemId(itSystemId);
						itSystem.setName(itSystemName);
						itSystem.setUserRoles(new ArrayList<>());

						itSystem = itSystemService.save(itSystem);
						updateRoles(itSystem, roles);
						created++;
					}

					itSystemService.save(itSystem);
				}

				List<ItSystem> toBeDeleted = new ArrayList<>();
				for (ItSystem itSystem : itSystems) {
					Optional<NewApiDTO> incomingItSystem = data.stream().filter(dto -> dto.itSystemId() == itSystem.getItSystemId()).findAny();
					if (incomingItSystem.isEmpty()) {
						toBeDeleted.add(itSystem);
						log.debug("Deleting ItSystem: " + itSystem.getName());
						deleted++;
					}
				}

				itSystemService.deleteAll(toBeDeleted);

				if (updated > 0 || created > 0 || deleted > 0) {
					log.debug("Sync Results Updated: " + updated + " Created: " + created + " Deleted: " + deleted);
				}
			}
			else {
				log.error("Body received from OS2rollekatalog was null.");
			}
		}
		catch (Exception ex) {
			log.error("Error occured while synchronizing data from OS2rollekatalog.", ex);
		}
	}

	private boolean updateRoles(ItSystem itSystem, List<UserRoleNewApiDTO> roles) {
		int updated = 0, created = 0, deleted = 0;
		for (UserRoleNewApiDTO userRoleDTO : roles) {
			Optional<UserRole> userRole = itSystem.getUserRoles().stream().filter(ur -> ur.getUserRoleId() == userRoleDTO.id()).findAny();
			if (userRole.isPresent()) {
				//Update
				UserRole existingUserRole = userRole.get();
				boolean hasChanges = false;

				if (!Objects.equals(existingUserRole.getName(), userRoleDTO.name())) {
					hasChanges = true;
				}
				if (!Objects.equals(existingUserRole.getDescription(), userRoleDTO.description())) {
					hasChanges = true;
				}

				if (hasChanges) {
					log.debug("\tUpdating UserRole: " + userRole.get().getName());

					existingUserRole.setName(userRoleDTO.name());
					existingUserRole.setDescription(userRoleDTO.description());

					updated++;
				}
			}
			else {
				//Create
				log.debug("\tCreating UserRole: " + userRoleDTO.name());
				UserRole newUserRole = new UserRole();
				newUserRole.setUserRoleId(userRoleDTO.id());
				newUserRole.setName(userRoleDTO.name());
				newUserRole.setDescription(userRoleDTO.description());
				newUserRole.setItSystem(itSystem);

				itSystem.getUserRoles().add(newUserRole);
				created++;
			}
		}

		//Delete UserRoles
		for (Iterator<UserRole> iterator = itSystem.getUserRoles().iterator(); iterator.hasNext();) {
			UserRole userRole = iterator.next();
			if (roles.stream().noneMatch(dto -> dto.id() == userRole.getUserRoleId())) {
				log.debug("\tDeleting UserRole: " + userRole.getName());
				iterator.remove();
				deleted++;
			}
		}

		log.debug("Updated: " + updated + " Created: " + created + " Deleted: " + deleted);

		return (updated != 0 || created != 0 || deleted != 0);
	}

	public void assignRolesToUsersInRc() {
		List<Employee> consultants = employeeService.findByIntegratedAndLoaded(EmployeeStatus.AWAITING_RC_INTEGRATION);
		boolean response;
		for (Employee consultant : consultants) {
			for (UserRole role : consultant.getUserRoles()) {
				if (StringUtils.hasText(consultant.getMitIdUuid())) {
					response = assignUserRoleToUser(role.getUserRoleId(), consultant.getMitIdUuid());
				} else {
					response = assignUserRoleToUser(role.getUserRoleId(), consultant.getUser().getName());
				}
				if (response) {
					log.debug("Assigned " + role.getUserRoleId() + " to " + (StringUtils.hasText(consultant.getMitIdUuid()) ? consultant.getMitIdUuid() : consultant.getUser().getName()));
				}
				else {
					log.debug("Failed to assign " + role.getUserRoleId() + " to " + (StringUtils.hasText(consultant.getMitIdUuid()) ? consultant.getMitIdUuid() : consultant.getUser().getName()));
					continue;
				}
				consultant.setIntegrated(EmployeeStatus.FULLY_INTEGRATED);
			}
			employeeService.save(consultant);
		}
	}

	private HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("ApiKey", configuration.getRc().getApiKey());
		headers.add("Content-Type", APPLICATION_JSON);
		headers.add("Accept", APPLICATION_JSON);

		return headers;
	}

	private List<NewApiDTO> fetchDataFromRC() throws Exception {
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = getHeaders();

		HttpEntity<List<NewApiDTO>> request = new HttpEntity<>(headers);

		ResponseEntity<List<NewApiDTO>> response = restTemplate.exchange(
				configuration.getRc().getUrl() + "/api/itSystem/managed", HttpMethod.GET, request,
				new ParameterizedTypeReference<List<NewApiDTO>>() {
				});

		if (response.getStatusCode() != HttpStatus.OK) {
			throw new RoleCatalogIntegrationException("Error connecting to RoleCatalog: " + configuration.getRc().getUrl() + " StatusCode: " + response.getStatusCode());
		}

		return response.getBody();
	}
}
