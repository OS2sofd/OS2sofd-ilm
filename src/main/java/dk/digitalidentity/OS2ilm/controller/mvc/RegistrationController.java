package dk.digitalidentity.OS2ilm.controller.mvc;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import dk.digitalidentity.OS2ilm.model.role.RequireEmployeeRole;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Optional;

@Controller
@RequireEmployeeRole
@RequiredArgsConstructor
public class RegistrationController {
    private final SecurityUtil securityUtil;
    private final IlmConfiguration configuration;
	private final BusinessService businessService;

	private static final String REDIRECT_AUTHENTICATION = "redirect:/saml2/authenticate/";

    @GetMapping("/register")
    public String register(Model model) {
        if (securityUtil.notAuthenticated()) {
            return REDIRECT_AUTHENTICATION + Base64.getEncoder().encodeToString(configuration.getIdps().getMitIdIdPEntityId().getBytes(Charset.forName("UTF-8")));
        }
		model.addAttribute("businesses", businessService.findAll());
		model.addAttribute("exists", businessService.doesBusinessExist(securityUtil.getTokenUser()));
		model.addAttribute("requirePhone", configuration.getCustomer().isRequirePhone());
		model.addAttribute("requireRegistrationReason", configuration.getCustomer().isRequireRegistrationReason());
		model.addAttribute("cvr", securityUtil.getTokenUser().getAttributes().get("https://data.gov.dk/model/core/eid/professional/cvr"));
		String uuid = (String) securityUtil.getTokenUser().getAttributes().get("https://data.gov.dk/model/core/eid/professional/uuid/persistent");
		model.addAttribute("uuid", uuid);

		Optional<String> verifiedName = securityUtil.getVerifiedName();
		model.addAttribute("nameFromMitId", verifiedName.orElse(null));
		model.addAttribute("nameLocked", configuration.getCustomer().isLockNameFromMitId() && verifiedName.isPresent());
		model.addAttribute("cpr", securityUtil.getCPRAttribute().orElse(null));

        return "registration/registration";
    }

	@GetMapping(value = {"/acceptTos", "/registration/policy"})
	public String acceptTos(Model model) {
		if (securityUtil.notAuthenticated()) {
			return REDIRECT_AUTHENTICATION + Base64.getEncoder().encodeToString(configuration.getIdps().getMitIdIdPEntityId().getBytes(Charset.forName("UTF-8")));
		}

		final Optional<Employee> employee = securityUtil.getEmployee();
		if (employee.isEmpty()) {
			return REDIRECT_AUTHENTICATION + Base64.getEncoder().encodeToString(configuration.getIdps().getMitIdIdPEntityId().getBytes(Charset.forName("UTF-8")));
		}
		model.addAttribute("employeeId", employee.get().getId());
		return "registration/policy";
	}

	@GetMapping("/registration/awaitingApproval/{id}")
	public String awaitingApproval(@PathVariable long id) {
		if (!securityUtil.isAuthenticated()) {
			return REDIRECT_AUTHENTICATION + Base64.getEncoder().encodeToString(configuration.getIdps().getMitIdIdPEntityId().getBytes(Charset.forName("UTF-8")));
		}

		final Optional<Employee> employee = securityUtil.getEmployee();
		if (employee.isEmpty()) {
			return REDIRECT_AUTHENTICATION + Base64.getEncoder().encodeToString(configuration.getIdps().getMitIdIdPEntityId().getBytes(Charset.forName("UTF-8")));
		}

		if (employee.get().getApproved().equals(ApproveStatus.LEADER_APPROVED) || employee.get().getApproved().equals(ApproveStatus.ADMIN_APPROVED)) {
			return "redirect:/employee/my/account/" + id;
		}
		return "registration/awaitingApproval";
	}
}
