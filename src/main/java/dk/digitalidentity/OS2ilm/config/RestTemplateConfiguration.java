package dk.digitalidentity.OS2ilm.config;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

@Slf4j
// explicit name, framework-s3fileshare ships a configuration class with the same simple name
@Configuration("ilmRestTemplateConfiguration")
public class RestTemplateConfiguration {

	@Bean
	@Primary
	public RestTemplate customRestTemplate(IlmConfiguration configuration) throws Exception {
		HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
		httpRequestFactory.setConnectionRequestTimeout(10000);
		httpRequestFactory.setReadTimeout(10000);

		if (configuration.isSslValidationDisabled() && configuration.isDevelopmentMode()) {
			log.error("SSL certificate validation is disabled - this should only be used in development");
			TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;

			SSLContext sslContext = SSLContextBuilder.create()
					.loadTrustMaterial(acceptingTrustStrategy)
					.build();

			// Create SSL connection socket factory with no hostname verification
			DefaultClientTlsStrategy clientTlsStrategy = new DefaultClientTlsStrategy(
					sslContext,
					NoopHostnameVerifier.INSTANCE);

			// Create HttpClient with custom SSL factory
			CloseableHttpClient httpClient = HttpClientBuilder.create()
					.setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
							.setTlsSocketStrategy(clientTlsStrategy)
							.build())
					.build();

			httpRequestFactory.setHttpClient(httpClient);
		}

		return new RestTemplate(httpRequestFactory);
	}
}
