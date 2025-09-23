package dk.digitalidentity.OS2ilm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfiguration {

	@Bean
	public RestTemplate customRestTemplate() {
		HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
		httpRequestFactory.setConnectionRequestTimeout(10000);
		httpRequestFactory.setConnectTimeout(10000);
		httpRequestFactory.setReadTimeout(10000);

		return new RestTemplate(httpRequestFactory);
	}
}
