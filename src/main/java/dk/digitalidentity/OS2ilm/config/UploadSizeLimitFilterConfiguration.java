package dk.digitalidentity.OS2ilm.config;

import dk.digitalidentity.OS2ilm.security.filter.UploadSizeLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class UploadSizeLimitFilterConfiguration {

	@Bean
	public FilterRegistrationBean<UploadSizeLimitFilter> uploadSizeLimitFilter() {
		FilterRegistrationBean<UploadSizeLimitFilter> filterRegistrationBean = new FilterRegistrationBean<>(new UploadSizeLimitFilter());
		filterRegistrationBean.setName("UploadSizeLimitFilter");
		filterRegistrationBean.addUrlPatterns("/admin/cms-edit/logo");
		// must run ahead of Spring Security's filter chain, since CsrfFilter can trigger
		// multipart parsing (and the size-limit exception) before DispatcherServlet is reached
		filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);

		return filterRegistrationBean;
	}
}
