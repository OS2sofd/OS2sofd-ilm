package dk.digitalidentity.OS2ilm.controller.mvc;

import dk.digitalidentity.OS2ilm.model.entity.ActivationTimeFrame;
import dk.digitalidentity.OS2ilm.model.entity.enums.AccountStatus;
import dk.digitalidentity.OS2ilm.model.role.RequireReadOnlyAdministrator;
import dk.digitalidentity.OS2ilm.model.role.RequireSystemAdministrator;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.ActivationTimeFrameService;
import dk.digitalidentity.OS2ilm.service.AdUserService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.cms.CmsMessageBundle;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailPlaceHolder;
import dk.digitalidentity.OS2ilm.service.cms.CmsMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Slf4j
@Controller
@RequireSystemAdministrator
@RequiredArgsConstructor
public class AdminController {
	private final CmsMessageBundle cmsMessageBundle;
	private final CmsMessageService cmsMessageService;
	private final AdUserService adUserService;
	private final ActivationTimeFrameService activationTimeFrameService;
	private final EmployeeService employeeService;
	private final SecurityUtil securityUtil;

	@RequireReadOnlyAdministrator
	@GetMapping("/admin/cms")
	public String admin(Model model) {
		model.addAttribute("cmsMessage", cmsMessageBundle.getAll());

		return "admin/cms/cms";
	}

	record CmsMessage(String key, String value, String description) { }

	@GetMapping("/admin/cms-edit")
	public String editCms(Model model, @RequestParam("key") String key) {
		CmsMessage dto = new CmsMessage(key, cmsMessageBundle.getText(key, true), cmsMessageBundle.getDescription(key));
		model.addAttribute("cmsMessage", dto);
		model.addAttribute("placeholders", EmailPlaceHolder.values());

		return "admin/cms/edit";
	}

	@PostMapping("/admin/cms-edit")
	public String saveCms(Model model, CmsMessage cmsMessage) {
		if (cmsMessage.value != null && cmsMessage.value.length() > 65536) {
			CmsMessage dto = new CmsMessage(cmsMessage.key, cmsMessage.value, cmsMessageBundle.getDescription(cmsMessage.key));
			model.addAttribute("cmsMessage", dto);
			model.addAttribute("showError", true);
			model.addAttribute("placeholders", EmailPlaceHolder.values());
			return "admin/cms/edit";
		}

		PolicyFactory policy = new HtmlPolicyBuilder()
				.allowCommonBlockElements()
				.allowCommonInlineFormattingElements()
				.allowElements("a")
				.allowUrlProtocols("https")
				.allowAttributes("href", "target").onElements("a")
				.allowAttributes("class", "style", "id", "name").globally()
				.toFactory();
		String safeHTML = policy.sanitize(cmsMessage.value);

		dk.digitalidentity.OS2ilm.model.entity.CmsMessage cms = cmsMessageService.getByCmsKey(cmsMessage.key);

		if (cms == null) {
			cms = new dk.digitalidentity.OS2ilm.model.entity.CmsMessage();
			cms.setCmsKey(cmsMessage.key);
		}

		cms.setCmsValue(safeHTML);
		cms.setLastUpdated(LocalDateTime.now());
		// TODO: Add logging
		cmsMessageService.save(cms);

		return "redirect:/admin/cms";
	}

	@PostMapping("/admin/cms-edit/logo")
	public String saveCms(@RequestParam("value") MultipartFile file, @RequestParam("key") String key, Model model) throws IOException {
		String base64 = Base64.getEncoder().encodeToString(file.getBytes());
		dk.digitalidentity.OS2ilm.model.entity.CmsMessage logo = cmsMessageService.getByCmsKey("cms.logo");

		if (logo == null) {
			logo = new dk.digitalidentity.OS2ilm.model.entity.CmsMessage();
			logo.setCmsKey("cms.logo");
		}

		if (base64.length() > 65536) {
			CmsMessage dto = new CmsMessage(logo.getCmsKey(), null, cmsMessageBundle.getDescription(logo.getCmsKey()));
			model.addAttribute("cmsMessage", dto);
			model.addAttribute("showError", true);

			return "admin/cms/edit";
		}


		logo.setCmsValue(base64);
		logo.setLastUpdated(LocalDateTime.now());
		// TODO: Add logging
		cmsMessageService.save(logo);

		return "redirect:/admin/cms";
	}

	record AdUserDTO(long id, String name, AccountStatus status, LocalDate startDate, LocalDate stopDate, String adUserNames) {}
	@RequireReadOnlyAdministrator
	@GetMapping("/admin/ad/accounts")
	@Transactional
	public String accounts(Model model) {
		List<AdUserDTO> userDTOS = adUserService.findAll().stream()
				.map(u -> new AdUserDTO(u.getId(), u.getName(), u.status, u.getStartDate(), u.getStopDate(), u.getEmployee() != null ? employeeService.listAdUsers(u.getEmployee()) : "" ))
				.toList();

		model.addAttribute("users", userDTOS);
		return "admin/ad/overview";
	}

	@RequireReadOnlyAdministrator
	@GetMapping("/admin/ad/activation")
	public String activations(Model model) {
		model.addAttribute("activations", activationTimeFrameService.findAll());
		model.addAttribute("isAdmin", securityUtil.isAdmin());
		model.addAttribute("isLeader", securityUtil.isLeader());
		return "admin/ad/activation";
	}

	@GetMapping("/admin/ad/activation/edit/{id}")
	public String activations(Model model, @PathVariable Long id) {
		ActivationTimeFrame activationTimeFrame = activationTimeFrameService.findById(id);
		model.addAttribute("activation", activationTimeFrame);
		return "admin/ad/editActivation";
	}

	@GetMapping("/admin/ad/activation/create")
	public String createActivation() {
		return "admin/ad/createActivation";
	}

}
