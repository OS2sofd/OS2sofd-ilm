package dk.digitalidentity.OS2ilm.controller.mvc;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import dk.digitalidentity.OS2ilm.model.role.NoRoleRequired;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoRoleRequired
@Controller
public class IlmErrorController implements ErrorController {
	private ErrorAttributes errorAttributes = new DefaultErrorAttributes();

    @RequestMapping(value = "/error", produces = "text/html")
    public String errorHTML(Model model, HttpServletRequest request, HttpServletResponse response) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        // get SAML exceptions
        Object authException = request.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		if (authException == null && request.getSession() != null) {
			authException = request.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		}
		if (authException instanceof Throwable) {
			exception = authException;
		}

        if (status != null && !String.valueOf(status).equals("404")) {
            request.getSession().invalidate();
            response.addHeader("Set-Cookie", "JSESSIONID=; Path=/; HttpOnly; Max-Age=0;");
        }

        // If there was a proper exception, we provide the trace
        String exceptionMessage = (exception != null) ? ((Throwable) exception).getMessage() : null;

        if (StringUtils.hasText(exceptionMessage)) {
        	log.warn("Exception message from controller layer: " + exceptionMessage);
        }
        
        model.addAttribute("status", String.valueOf(status));
        model.addAttribute("timeStamp", LocalDateTime.now());
        model.addAttribute("location", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
        model.addAttribute("exception", exceptionMessage);

        return "error/default";
    }

	@RequestMapping(value = "/error", produces = "application/json")
	public ResponseEntity<Map<String, Object>> errorJSON(HttpServletRequest request) {
		Map<String, Object> body = getErrorAttributes(new ServletWebRequest(request));

		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		try {
			status = HttpStatus.valueOf((int) body.get("status"));
		}
		catch (Exception ex) {
			log.error("Exception occurred while getting status from request", ex);
		}

		return new ResponseEntity<>(body, status);
	}

	private Map<String, Object> getErrorAttributes(WebRequest request) {
		return errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());
	}
}
