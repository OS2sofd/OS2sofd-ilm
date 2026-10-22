package dk.digitalidentity.OS2ilm.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
	private final IlmConfiguration configuration;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS));
			if(!configuration.isDevelopmentMode()) {
				registry.addResourceHandler("/**")
						.addResourceLocations("classpath:/static/")
						.setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));
			}

    }
}
