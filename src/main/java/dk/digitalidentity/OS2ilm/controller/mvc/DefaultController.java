package dk.digitalidentity.OS2ilm.controller.mvc;

import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import dk.digitalidentity.OS2ilm.model.role.NoRoleRequired;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@NoRoleRequired
@RequiredArgsConstructor
public class DefaultController {
	private final SecurityUtil securityUtil;

	@GetMapping("/")
	public String index() {
		return "redirect:/dashboard";
	}

	@GetMapping("/dashboard")
	public String dashboard(HttpServletRequest request) {
		if (securityUtil.isAdmin() || securityUtil.isReadOnlyAdmin() || securityUtil.isLeader()) {
			return "redirect:/employees/overview";
		}

		final Optional<Employee> employee = securityUtil.getEmployee();
		if (employee.isPresent()) {
			return resolveEmployeeDestination(employee.get());
		}

		return resolveUnknownUserDestination(request);
	}

	@GetMapping("/login/saml/{type}/{id}")
	public String loginRedirect(@PathVariable String type, @PathVariable String id, HttpServletRequest request) {
		request.getSession().setAttribute("type", type);
		return "redirect:/saml2/authenticate/" + id;
	}

	private String resolveEmployeeDestination(Employee employee) {
		if (!employee.isAcceptedTos()) {
			return "redirect:/registration/policy";
		}

		if (employee.getApproved().equals(ApproveStatus.AWAITING_APPROVAL)) {
			return "registration/awaitingApproval";
		}
		return "redirect:/employee/my/account/" + employee.getId();
	}

	private String resolveUnknownUserDestination(HttpServletRequest request) {
		String loginType = (String) request.getSession().getAttribute("type");
		if ("leader".equals(loginType) || "admin".equals(loginType)) {
			return "error/unknown-user";
		}
		return "redirect:/register";
	}
}
