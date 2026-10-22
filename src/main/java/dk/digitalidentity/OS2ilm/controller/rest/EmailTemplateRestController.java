package dk.digitalidentity.OS2ilm.controller.rest;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import dk.digitalidentity.OS2ilm.model.entity.EmailTemplate;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplateType;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.model.role.RequireSystemAdministrator;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.AttachmentService;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.EmailTemplateService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Hidden
@RequestMapping("/rest/admin/emailtemplate")
@RestController
@RequiredArgsConstructor
@RequireSystemAdministrator
public class EmailTemplateRestController {
	private static final long MAX_ATTACHMENT_SIZE_BYTES = 10L * 1024 * 1024;
	private static final Set<String> ALLOWED_ATTACHMENT_EXTENSIONS = Set.of("pdf", "doc", "docx", "odt", "xls", "xlsx", "ods", "txt", "png", "jpg", "jpeg");

	private final EmailTemplateService emailTemplateService;
	private final AttachmentService attachmentService;
	private final AuditLogService auditLogService;
	private final SecurityUtil securityUtil;

	public record EmailTemplateLogRecord(long id, EmailTemplateType templateType, String subject, String content, boolean enabled) {}
	public record SetEnabledRecord(boolean enabled) {}
	public record TestEmailRecord(String subject, String content) {}
	public record TestEmailResponse(String message, String warning) {}

	@PostMapping("/{id}/enabled")
	public ResponseEntity<Void> setEnabled(@PathVariable long id, @RequestBody SetEnabledRecord record) {
		EmailTemplate emailTemplate = emailTemplateService.findById(id);
		if (emailTemplate == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mailskabelonen findes ikke");
		}

		emailTemplate.setEnabled(record.enabled());
		EmailTemplate saved = emailTemplateService.save(emailTemplate);

		LogAction logAction = saved.isEnabled() ? LogAction.EMAIL_TEMPLATE_ACTIVATED : LogAction.EMAIL_TEMPLATE_DEACTIVATED;
		String message = saved.isEnabled() ? "Mailskabelon aktiveret" : "Mailskabelon deaktiveret";
		auditLogService.log(securityUtil.getPerformerId(), securityUtil.getName(), saved.getId(), saved.getTemplateType().getTitle(), logAction, message,
				new EmailTemplateLogRecord(saved.getId(), saved.getTemplateType(), saved.getSubject(), saved.getContent(), saved.isEnabled()));

		return ResponseEntity.ok().build();
	}

	@PostMapping("/{id}/attachment")
	public ResponseEntity<?> uploadAttachment(@PathVariable long id, @RequestParam("file") MultipartFile file) {
		EmailTemplate emailTemplate = emailTemplateService.findById(id);
		if (emailTemplate == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mailskabelonen findes ikke");
		}

		String error = validateAttachment(file);
		if (error != null) {
			log.warn("Invalid attachment submitted for email template {}: {}", emailTemplate.getTemplateType(), error);
			return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
		}

		String previousKey = emailTemplate.getS3AttachmentKey();
		String newKey = attachmentService.uploadEmailTemplateAttachment(file);

		emailTemplate.setS3AttachmentKey(newKey);
		EmailTemplate saved = emailTemplateService.save(emailTemplate);

		// the old file is only scheduled for deletion after the new key is safely saved, so a template never ends up without an attachment because of this
		attachmentService.scheduleEmailTemplateAttachmentDeletion(previousKey);

		auditLogService.log(securityUtil.getPerformerId(), securityUtil.getName(), saved.getId(), saved.getTemplateType().getTitle(), LogAction.EMAIL_TEMPLATE_UPDATED, "Fil vedhæftet til mailskabelon",
				new EmailTemplateLogRecord(saved.getId(), saved.getTemplateType(), saved.getSubject(), saved.getContent(), saved.isEnabled()));

		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{id}/attachment")
	public ResponseEntity<Void> deleteAttachment(@PathVariable long id) {
		EmailTemplate emailTemplate = emailTemplateService.findById(id);
		if (emailTemplate == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mailskabelonen findes ikke");
		}

		String previousKey = emailTemplate.getS3AttachmentKey();
		emailTemplate.setS3AttachmentKey(null);
		EmailTemplate saved = emailTemplateService.save(emailTemplate);

		attachmentService.scheduleEmailTemplateAttachmentDeletion(previousKey);

		auditLogService.log(securityUtil.getPerformerId(), securityUtil.getName(), saved.getId(), saved.getTemplateType().getTitle(), LogAction.EMAIL_TEMPLATE_UPDATED, "Vedhæftet fil fjernet fra mailskabelon",
				new EmailTemplateLogRecord(saved.getId(), saved.getTemplateType(), saved.getSubject(), saved.getContent(), saved.isEnabled()));

		return ResponseEntity.ok().build();
	}

	private static String validateAttachment(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			return "Ingen fil valgt";
		}

		String filename = file.getOriginalFilename();
		String extension = filename != null && filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
		if (!ALLOWED_ATTACHMENT_EXTENSIONS.contains(extension)) {
			return "Filtypen er ikke tilladt";
		}

		if (file.getSize() > MAX_ATTACHMENT_SIZE_BYTES) {
			return "Filen er for stor. Der tillades max " + (MAX_ATTACHMENT_SIZE_BYTES / (1024 * 1024)) + " MB";
		}

		return null;
	}

	@PostMapping("/{id}/test")
	public ResponseEntity<?> sendTestEmail(@PathVariable long id, @RequestBody TestEmailRecord record) {
		EmailTemplate emailTemplate = emailTemplateService.findById(id);
		if (emailTemplate == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mailskabelonen findes ikke");
		}

		Optional<String> email = securityUtil.getEmail();
		if (email.isEmpty()) {
			log.warn("Test email for {} requested by {}, but the user has no email address", emailTemplate.getTemplateType(), securityUtil.getName());
			return new ResponseEntity<>("Du har ingen email adresse registreret!", HttpStatus.CONFLICT);
		}

		String error = emailTemplateService.validate(emailTemplate, record.subject(), record.content());
		if (error != null) {
			log.warn("Invalid email template {} submitted for test email: {}", emailTemplate.getTemplateType(), error);
			return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
		}

		long secondsToWait = emailTemplateService.reserveTestEmail(email.get());
		if (secondsToWait > 0) {
			log.warn("Test email for {} requested by {} within the rate limit, {} seconds left", emailTemplate.getTemplateType(), securityUtil.getName(), secondsToWait);
			return new ResponseEntity<>("Du kan kun sende én test email hvert " + EmailTemplateService.TEST_EMAIL_INTERVAL.toSeconds() + ". sekund. Prøv igen om " + secondsToWait + " sekunder", HttpStatus.TOO_MANY_REQUESTS);
		}

		// the test email is sent with the sanitized content, so tell the admin if something was removed
		String warning = emailTemplateService.getRemovedHtmlWarning(emailTemplateService.sanitize(record.content()));

		return switch (emailTemplateService.sendTestEmail(emailTemplate, record.subject(), record.content(), email.get())) {
			case SENT -> ResponseEntity.ok(new TestEmailResponse("Test email sendt til " + email.get(), warning));
			case LOGGED -> ResponseEntity.ok(new TestEmailResponse("Udviklingstilstand er slået til - test email til " + email.get() + " er logget i stedet for at blive sendt", warning));
			case FAILED -> {
				log.error("Failed to send test email for {} to {}", emailTemplate.getTemplateType(), email.get());
				yield new ResponseEntity<>("Test emailen kunne ikke sendes. Kontakt venligst support", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		};
	}
}
