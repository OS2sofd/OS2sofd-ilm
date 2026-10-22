package dk.digitalidentity.OS2ilm.util;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;

@Slf4j
public class QueueUtils {
	private static final String APPLICATION_JSON = "application/json";

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static String objectToJson(Object object) throws JacksonException {
		String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
		log.debug("Converted object to JSON: length={}", json.length());
		return json;
	}

	public static HttpHeaders getSofdHeaders(String apiKey) {
		log.debug("Creating HTTP headers for SOFD API request");
		HttpHeaders headers = new HttpHeaders();
		headers.add("ApiKey", apiKey);
		headers.add("Content-Type", APPLICATION_JSON);
		headers.add("Accept", APPLICATION_JSON);
		return headers;
	}

	/**
	 * Masks CPR number for logging purposes
	 */
	public static String maskCpr(String cpr) {
		return "REDACTED_CPR";
	}
}
