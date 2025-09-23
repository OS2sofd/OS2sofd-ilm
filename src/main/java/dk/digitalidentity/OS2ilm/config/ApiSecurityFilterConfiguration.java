package dk.digitalidentity.OS2ilm.config;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApiRole;
import dk.digitalidentity.OS2ilm.security.filter.ApiSecurityFilter;
import dk.digitalidentity.OS2ilm.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ApiSecurityFilterConfiguration {
	private final IlmConfiguration configuration;
	private final ClientService clientService;

	@Bean
	public FilterRegistrationBean<ApiSecurityFilter> internalApiSecurityFilter() {
		ApiSecurityFilter filter = new ApiSecurityFilter();
		filter.setClientService(clientService);
		filter.setApiRole(ApiRole.INTERNAL);
		filter.setEnabled(configuration.getApi().getInternal().isEnabled());

		FilterRegistrationBean<ApiSecurityFilter> filterRegistrationBean = new FilterRegistrationBean<>(filter);
		filterRegistrationBean.setName("InternalApiSecurityBean");
		filterRegistrationBean.addUrlPatterns("/api/internal", "/api/internal/*");

		return filterRegistrationBean;
	}

	@Bean
	public FilterRegistrationBean<ApiSecurityFilter> employeeApiSecurityFilter() {
		ApiSecurityFilter filter = new ApiSecurityFilter();
		filter.setClientService(clientService);
		filter.setApiRole(ApiRole.EMPLOYEE);
		filter.setEnabled(configuration.getApi().getInternal().isEnabled());

		FilterRegistrationBean<ApiSecurityFilter> filterRegistrationBean = new FilterRegistrationBean<>(filter);
		filterRegistrationBean.setName("EmployeeApiSecurityBean");
		filterRegistrationBean.addUrlPatterns("/api/employee", "/api/employee/*");

		return filterRegistrationBean;
	}

	@Bean
	public FilterRegistrationBean<ApiSecurityFilter> businessApiSecurityFilter() {
		ApiSecurityFilter filter = new ApiSecurityFilter();
		filter.setClientService(clientService);
		filter.setApiRole(ApiRole.BUSINESS);
		filter.setEnabled(configuration.getApi().getInternal().isEnabled());

		FilterRegistrationBean<ApiSecurityFilter> filterRegistrationBean = new FilterRegistrationBean<>(filter);
		filterRegistrationBean.setName("BusinessApiSecurityBean");
		filterRegistrationBean.addUrlPatterns("/api/business", "/api/business/*");

		return filterRegistrationBean;
	}
}
