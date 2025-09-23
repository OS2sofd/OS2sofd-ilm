package dk.digitalidentity.OS2ilm.controller.mvc;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import dk.digitalidentity.OS2ilm.model.role.NoRoleRequired;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.charset.Charset;
import java.util.Base64;

@Controller
@NoRoleRequired
@RequiredArgsConstructor
public class RegistrationController {
    private final SecurityUtil securityUtil;
    private final IlmConfiguration configuration;
	private final BusinessService businessService;

    @GetMapping("/register")
    public String register(Model model) {
        if (!securityUtil.isAuthenticated()) {
            return "redirect:/saml2/authenticate/" + Base64.getEncoder().encodeToString(configuration.getIdps().getMitIdIdPEntityId().getBytes(Charset.forName("UTF-8")));
        }
		model.addAttribute("businesses", businessService.findAll());
		model.addAttribute("exists", businessService.doesBusinessExist(securityUtil.getTokenUser()));
		if (securityUtil.getTokenUser() != null) {
			model.addAttribute("cvr", securityUtil.getTokenUser().getAttributes().get("https://data.gov.dk/model/core/eid/professional/cvr"));
			String uuid = (String) securityUtil.getTokenUser().getAttributes().get("https://data.gov.dk/model/core/eid/professional/uuid/persistent");
			model.addAttribute("uuid", uuid);
		}

        return "registration/registration";
    }

	@GetMapping(value = {"/acceptTos", "/registration/policy"})
	public String acceptTos(Model model) {
		if (!securityUtil.isAuthenticated()) {
			return "redirect:/saml2/authenticate/" + Base64.getEncoder().encodeToString(configuration.getIdps().getMitIdIdPEntityId().getBytes(Charset.forName("UTF-8")));
		}
		return "registration/policy";
	}

	@GetMapping("/registration/awaitingApproval/{id}")
	public String awaitingApproval(Model model, @PathVariable long id) {
		if (!securityUtil.isAuthenticated()) {
			return "redirect:/saml2/authenticate/" + Base64.getEncoder().encodeToString(configuration.getIdps().getMitIdIdPEntityId().getBytes(Charset.forName("UTF-8")));
		}
		if (!securityUtil.getEmployee().getApproved().equals(ApproveStatus.AWAITING_APPROVAL)) {
			return "redirect:/employee/my/account/" + id;
		}
		return "registration/awaitingApproval";
	}
}
