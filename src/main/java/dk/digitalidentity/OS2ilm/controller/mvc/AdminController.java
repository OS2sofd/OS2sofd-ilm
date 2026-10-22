package dk.digitalidentity.OS2ilm.controller.mvc;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.controller.rest.EmailTemplateRestController.EmailTemplateLogRecord;
import dk.digitalidentity.OS2ilm.model.entity.CmsMessage;
import dk.digitalidentity.OS2ilm.model.entity.EmailTemplate;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.model.entity.dto.CmsMessageRecord;
import dk.digitalidentity.OS2ilm.service.ChangelogService;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import dk.digitalidentity.OS2ilm.model.entity.ActivationTimeFrame;
import dk.digitalidentity.OS2ilm.model.entity.enums.AccountStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.CmsType;
import dk.digitalidentity.OS2ilm.model.entity.enums.CmsProperty;
import dk.digitalidentity.OS2ilm.model.role.RequireReadOnlyAdministrator;
import dk.digitalidentity.OS2ilm.model.role.RequireSystemAdministrator;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.ActivationTimeFrameService;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.EmailTemplateService;
import dk.digitalidentity.OS2ilm.service.cms.CmsMessageBundle;
import dk.digitalidentity.OS2ilm.service.cms.CmsMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequireSystemAdministrator
@RequiredArgsConstructor
public class AdminController {
	private final CmsMessageBundle cmsMessageBundle;
	private final CmsMessageService cmsMessageService;
	private final ActivationTimeFrameService activationTimeFrameService;
	private final SecurityUtil securityUtil;
	private final EmailTemplateService emailTemplateService;
	private final AuditLogService auditLogService;
	private final IlmConfiguration configuration;
	private final ChangelogService changelogService;

	@RequireReadOnlyAdministrator
	@GetMapping("/admin/cms")
	public String admin(Model model) {
		model.addAttribute("cmsText", cmsMessageBundle.getByType(CmsType.TEXT));

		return "admin/cms/cms";
	}

	@GetMapping("/admin/cms-edit")
	public String editCms(Model model, @RequestParam("key") String key, @RequestParam(value = "fileTooLarge", required = false) Boolean fileTooLarge) {
		CmsMessageRecord dto = new CmsMessageRecord(key, cmsMessageBundle.getText(key, true), cmsMessageBundle.getDescription(key));
		model.addAttribute("cmsMessage", dto);
		model.addAttribute("showError", fileTooLarge);

		return "admin/cms/edit";
	}

	@PostMapping("/admin/cms-edit")
	public String saveCms(Model model, CmsMessageRecord cmsMessage) {
		if (cmsMessage.value() != null && cmsMessage.value().length() > 65536) {
			CmsMessageRecord dto = new CmsMessageRecord(cmsMessage.key(), cmsMessage.value(), cmsMessageBundle.getDescription(cmsMessage.key()));
			model.addAttribute("cmsMessage", dto);
			model.addAttribute("showError", true);
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
		String safeHTML = policy.sanitize(cmsMessage.value());

		CmsMessage cms = cmsMessageService.getByCmsKey(cmsMessage.key());

		if (cms == null) {
			cms = new CmsMessage();
			cms.setCmsKey(cmsMessage.key());
		}

		cms.setCmsValue(safeHTML);
		cms.setLastUpdated(LocalDateTime.now());
		cmsMessageService.save(cms);

		return "redirect:/admin/cms";
	}

	@PostMapping("/admin/cms-edit/logo")
	public String saveCms(@RequestParam("value") MultipartFile file, @RequestParam("key") String key, Model model) throws IOException {
		String base64 = Base64.getEncoder().encodeToString(file.getBytes());
		CmsMessage logo = cmsMessageService.getByCmsKey(CmsProperty.LOGO.getKey());

		if (logo == null) {
			logo = new CmsMessage();
			logo.setCmsKey(CmsProperty.LOGO.getKey());
		}

		if (base64.length() > 65536) {
			CmsMessageRecord dto = new CmsMessageRecord(logo.getCmsKey(), null, cmsMessageBundle.getDescription(logo.getCmsKey()));
			model.addAttribute("cmsMessage", dto);
			model.addAttribute("showError", true);

			return "admin/cms/edit";
		}


		logo.setCmsValue(base64);
		logo.setLastUpdated(LocalDateTime.now());
		cmsMessageService.save(logo);

		return "redirect:/admin/cms";
	}

	record AdUserDTO(long id, String name, AccountStatus status, LocalDate startDate, LocalDate stopDate, String adUserNames) {}
	@RequireReadOnlyAdministrator
	@GetMapping("/admin/ad/accounts")
	@Transactional
	public String accounts(Model model) {
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

	@GetMapping("/admin/invitation/form")
	public String invitationForm() {
		return "admin/invitation/invitationForm";
	}

	@RequireReadOnlyAdministrator
	@GetMapping("/admin/emailtemplate/list")
	public String emailTemplates(Model model) {
		model.addAttribute("templates", emailTemplateService.findAll());

		return "admin/emailtemplate/list";
	}

	public record EmailTemplateForm(long id, String subject, String content) {}

	@GetMapping("/admin/emailtemplate/edit/{id}")
	public String editEmailTemplate(Model model, @PathVariable long id) {
		EmailTemplate emailTemplate = getEmailTemplate(id);
		populateEmailTemplateModel(model, emailTemplate, new EmailTemplateForm(emailTemplate.getId(), emailTemplate.getSubject(), emailTemplate.getContent()), null, null);

		return "admin/emailtemplate/edit";
	}

	@PostMapping("/admin/emailtemplate/edit")
	public String saveEmailTemplate(Model model, EmailTemplateForm form) {
		EmailTemplate emailTemplate = getEmailTemplate(form.id());

		String error = emailTemplateService.validate(emailTemplate, form.subject(), form.content());
		if (error != null) {
			log.warn("Invalid email template {} submitted: {}", emailTemplate.getTemplateType(), error);
			populateEmailTemplateModel(model, emailTemplate, form, error, null);

			return "admin/emailtemplate/edit";
		}

		// do not save content the sanitizer changed - show the admin the cleaned version first, so nothing disappears silently
		EmailTemplateService.SanitizedContent sanitizedContent = emailTemplateService.sanitize(form.content());
		if (sanitizedContent.hasRemoved()) {
			log.warn("Email template {} submitted with disallowed HTML, not saved: {}", emailTemplate.getTemplateType(), sanitizedContent.removed());
			EmailTemplateForm sanitizedForm = new EmailTemplateForm(form.id(), form.subject(), sanitizedContent.content());
			populateEmailTemplateModel(model, emailTemplate, sanitizedForm, null, emailTemplateService.getRemovedHtmlWarning(sanitizedContent) + " Skabelonen er ikke gemt endnu - gem igen for at gemme den rensede version.");

			return "admin/emailtemplate/edit";
		}

		emailTemplate.setSubject(form.subject().trim());
		emailTemplate.setContent(sanitizedContent.content());
		EmailTemplate saved = emailTemplateService.save(emailTemplate);

		auditLogService.log(securityUtil.getPerformerId(), securityUtil.getName(), saved.getId(), saved.getTemplateType().getTitle(), LogAction.EMAIL_TEMPLATE_UPDATED, "Mailskabelon opdateret",
				new EmailTemplateLogRecord(saved.getId(), saved.getTemplateType(), saved.getSubject(), saved.getContent(), saved.isEnabled()));

		return "redirect:/admin/emailtemplate/list";
	}

	private EmailTemplate getEmailTemplate(long id) {
		EmailTemplate emailTemplate = emailTemplateService.findById(id);
		if (emailTemplate == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mailskabelonen findes ikke");
		}

		return emailTemplate;
	}

	private void populateEmailTemplateModel(Model model, EmailTemplate emailTemplate, EmailTemplateForm form, String error, String warning) {
		model.addAttribute("template", emailTemplate);
		model.addAttribute("form", form);
		model.addAttribute("placeholders", emailTemplate.getTemplateType().getPlaceholders());
		model.addAttribute("maxSubjectLength", EmailTemplateService.MAX_SUBJECT_LENGTH);
		model.addAttribute("error", error);
		model.addAttribute("warning", warning);
		model.addAttribute("isS3Enabled", StringUtils.hasLength(configuration.getCustomer().getS3FileBasePrefix()));
	}

	@RequireReadOnlyAdministrator
	@GetMapping("/admin/changelog")
	public String changelog(Model model) {
		model.addAttribute("changelogHtml", changelogService.getHtml());

		return "admin/changelog";
	}
}
