package dk.digitalidentity.OS2ilm.service;

import static dk.digitalidentity.OS2ilm.util.NullSafe.nullSafe;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.controller.api.model.GetBusinessResult;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@EnableCaching
public class CvrService {

	@Autowired
	private IlmConfiguration configuration;

	@Autowired
	private BusinessService businessService;


	@CacheEvict(value = "getSearchResultByCvr", allEntries = true)
	public void resetCvrSearchResultCache() {
		; // clears cache every hour
	}

	@Builder
	public record CvrSearchResultDTO(String cvr, String name, boolean leaderEnabled) {}
	@Cacheable(value = "getSearchResultByCvr")
	public Optional<CvrSearchResultDTO> getSearchResultByCvr(final String cvr) {
		if (!configuration.getIntegrations().getCvr().isEnabled()
				|| invalidCvr(cvr)) {
			return Optional.empty();
		}

		final Optional<GetBusinessResult> response = performGetRequest(new ParameterizedTypeReference<>() {}, "CVR/HentCVRData/1/rest/hentVirksomhedMedCVRNummer?pCVRNummer=" + cvr);
		return response
				.map(r -> CvrSearchResultDTO.builder()
						.cvr(nullSafe(() -> r.getVirksomhed().getCvr()))
						.name(nullSafe(() -> r.getVirksomhedsnavn().getName()))
						.leaderEnabled(businessService.checkLeaderEnabled(r.getVirksomhed().getCvr()))
						.build());
	}

	private static boolean invalidCvr(final String cvr) {
		if (cvr == null || cvr.length() != 8) {
			return true;
		}
		return !StringUtils.isNumeric(cvr);
	}

	private HttpHeaders getHeaders(String apiKey) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("apiKey", apiKey);
		headers.add("Content-Type", "application/json");

		return headers;
	}

	private String getCvrResourceUrl() {
		String cvrResourceUrl = configuration.getIntegrations().getCvr().getBaseUrl();
		if (!cvrResourceUrl.endsWith("/")) {
			cvrResourceUrl += "/";
		}
		return cvrResourceUrl;
	}

	private <T> Optional<T> performGetRequest(final ParameterizedTypeReference<T> responseType, final String urlPostfix) {
		try {
			final RestTemplate restTemplate = new RestTemplate();
			final String cvrResourceUrl = getCvrResourceUrl() + urlPostfix;
			final String apiKey = configuration.getIntegrations().getCvr().getApiKey();
			final HttpEntity<Object> request = new HttpEntity<>(getHeaders(apiKey));
			final ResponseEntity<T> exchange = restTemplate.exchange(cvrResourceUrl, HttpMethod.GET, request, responseType);
			return Optional.ofNullable(exchange.getBody());
		} catch (RestClientException ex) {
			log.warn("Failed to lookup: " + urlPostfix, ex);
			return Optional.empty();
		}
	}
}
