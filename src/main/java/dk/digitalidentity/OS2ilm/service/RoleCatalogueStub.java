package dk.digitalidentity.OS2ilm.service;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.exception.RoleCatalogIntegrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleCatalogueStub {
	private static final String APPLICATION_JSON = "application/json";
	private static final RestTemplate restTemplate = new RestTemplate();

	private final IlmConfiguration configuration;

	public record UserRoleDTO(long id, String name, String description) {}
	public record ItSystemDTO(long itSystemId, String itSystemName, List<UserRoleDTO> roles) {}
	public record RoleGroupDTO(long id, String name, String description) {}

	public boolean assignUserRoleToUser(long userRoleId, String userUuid) {
		log.info("Assigning userRole " + userRoleId + " to " + userUuid);

		HttpHeaders headers = getHeaders();
		HttpEntity<String> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(
				configuration.getRc().getUrl() + "/api/user/" + userUuid + "/assign/userrole/" + userRoleId,
				HttpMethod.PUT,
				request,
				String.class
			);

			if (response.getStatusCode() == HttpStatus.OK) {
				return true;
			}
		}
		catch (HttpClientErrorException ex) {
			if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
				log.warn(ex.getResponseBodyAsString());
			}
		}

		return false;
	}

	public boolean removeUserRoleFromUser(long userRoleId, String userUuid) {
		log.info("Deassigning userRole " + userRoleId + " from " + userUuid);
		
		HttpHeaders headers = getHeaders();
		HttpEntity<String> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(
				configuration.getRc().getUrl() + "/api/user/" + userUuid + "/deassign/userrole/" + userRoleId,
				HttpMethod.DELETE,
				request,
				String.class
			);

			if (response.getStatusCode() == HttpStatus.OK) {
				return true;
			}
		}
		catch (HttpClientErrorException ex) {
			if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
				log.warn(ex.getResponseBodyAsString());
				return true;
			}
		}
		
		return false;
	}

	public boolean removeRoleGroupFromUser(long roleGroupId, String userUuid) {
		log.info("Deassigning rolegroup " + roleGroupId + " from " + userUuid);
		
		HttpHeaders headers = getHeaders();
		HttpEntity<String> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(
				configuration.getRc().getUrl() + "/api/user/" + userUuid + "/deassign/rolegroup/" + roleGroupId,
				HttpMethod.DELETE,
				request,
				String.class
			);

			if (response.getStatusCode() == HttpStatus.OK) {
				return true;
			}
		}
		catch (HttpClientErrorException ex) {
			if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
				log.warn(ex.getResponseBodyAsString());
				return true;
			}
		}
		
		return false;
	}

	public boolean assignRoleGroupToUser(long roleGroupId, String userUuid) {
		log.info("Assigning rolegroup " + roleGroupId + " to " + userUuid);

		HttpHeaders headers = getHeaders();
		HttpEntity<String> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(
				configuration.getRc().getUrl() + "/api/user/" + userUuid + "/assign/rolegroup/" + roleGroupId,
				HttpMethod.PUT,
				request,
				String.class
			);

			if (response.getStatusCode() == HttpStatus.OK) {
				return true;
			}
		}
		catch (HttpClientErrorException ex) {
			if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
				log.warn(ex.getResponseBodyAsString());
			}
		}

		return false;
	}

	public List<RoleGroupDTO> getAllRoleGroups() {
		HttpHeaders headers = getHeaders();
		HttpEntity<List<ItSystemDTO>> request = new HttpEntity<>(headers);

		ResponseEntity<List<RoleGroupDTO>> response = restTemplate.exchange(
			configuration.getRc().getUrl() + "/api/v2/rolegroup",
			HttpMethod.GET,
			request,
			new ParameterizedTypeReference<List<RoleGroupDTO>>() {
		});

		if (response.getStatusCode() != HttpStatus.OK) {
			throw new RoleCatalogIntegrationException("Error connecting to RoleCatalog: " + configuration.getRc().getUrl() + " StatusCode: " + response.getStatusCode());
		}

		return response.getBody();
	}
	
	public List<ItSystemDTO> getItSystems() throws RoleCatalogIntegrationException {
		HttpHeaders headers = getHeaders();
		HttpEntity<List<ItSystemDTO>> request = new HttpEntity<>(headers);

		ResponseEntity<List<ItSystemDTO>> response = restTemplate.exchange(
			configuration.getRc().getUrl() + "/api/itSystem/managed",
			HttpMethod.GET,
			request,
			new ParameterizedTypeReference<List<ItSystemDTO>>() {
		});

		if (response.getStatusCode() != HttpStatus.OK) {
			throw new RoleCatalogIntegrationException("Error connecting to RoleCatalog: " + configuration.getRc().getUrl() + " StatusCode: " + response.getStatusCode());
		}

		return response.getBody();
	}
	
	private HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("ApiKey", configuration.getRc().getApiKey());
		headers.add("Content-Type", APPLICATION_JSON);
		headers.add("Accept", APPLICATION_JSON);

		return headers;
	}
}
