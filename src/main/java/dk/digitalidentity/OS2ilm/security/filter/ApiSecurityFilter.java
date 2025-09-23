package dk.digitalidentity.OS2ilm.security.filter;

import java.io.IOException;
import java.util.Objects;

import dk.digitalidentity.OS2ilm.model.entity.Client;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApiRole;
import dk.digitalidentity.OS2ilm.service.ClientService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class ApiSecurityFilter implements Filter {

    @Setter
    private ApiRole apiRole;

    @Setter
    private ClientService clientService;

    @Setter
    private boolean enabled;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String authHeader = request.getHeader("ApiKey");

        if (!enabled) {
            unauthorized(response, "API (" + apiRole.toString() + ") is not enabled", authHeader);
            return;
        }

        if (StringUtils.hasLength(authHeader)) {
            boolean apiKeyMatch = checkApiKey(authHeader);

            if (!apiKeyMatch) {
                unauthorized(response, "Invalid ApiKey for " + apiRole.toString(), authHeader);
                return;
            }

            filterChain.doFilter(servletRequest, servletResponse);
        }
        else {
            unauthorized(response, "Missing ApiKey for " + apiRole.toString(), null);
        }
    }

    private void unauthorized(HttpServletResponse response, String message, String authHeader) throws IOException {
        log.warn(message + " (authHeader = " + authHeader + ")");

        response.sendError(401, message);
    }

    private boolean checkApiKey(String apiKey) {
        Client entry = clientService.findByApiKey(apiKey);
        return entry != null && Objects.equals(entry.getRole(), apiRole);
    }
}
