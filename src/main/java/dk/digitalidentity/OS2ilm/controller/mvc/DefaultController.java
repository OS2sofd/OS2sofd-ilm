package dk.digitalidentity.OS2ilm.controller.mvc;

import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.role.NoRoleRequired;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
	public String dashboard() {
		Employee employee = securityUtil.getEmployee();
		if (securityUtil.isAdmin() || securityUtil.isReadOnlyAdmin() || securityUtil.isLeader()) {
			return "redirect:/employees/overview";
		}
		if (employee != null) {
			if (!employee.isAcceptedTos()) {
				return "redirect:/registration/policy";
			} else {
				return "redirect:/employee/my/account/" + employee.getId();
			}
		}
		return "redirect:/register";
	}

	@GetMapping("/login/saml/{type}/{id}")
	public String loginRedirect(@PathVariable String type, @PathVariable String id, HttpServletRequest request) {
		request.getSession().setAttribute("type", type);
		return "redirect:/saml2/authenticate/" + id;
	}
}
