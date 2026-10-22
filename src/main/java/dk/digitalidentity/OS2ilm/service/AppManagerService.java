package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.entity.ApplicationApiDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppManagerService {

	public List<ApplicationApiDTO> getApplications(String appManagerUrl) {
		RestClient restClient = RestClient.builder()
			.defaultHeader("Content-Type", "application/json")
			.build();

		appManagerUrl += "applications";
		try {
			return restClient
				.get()
				.uri(appManagerUrl)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {});
		} catch (RestClientException ex) {
			log.warn("Failed to call AppManager API.", ex);
			return null;
		}
	}
}
