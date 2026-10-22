package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.service.AttachmentService.FileAttachment;
import dk.digitalidentity.OS2ilm.service.EmailTemplateService.RenderedEmail;
import dk.digitalidentity.OS2ilm.service.eboks.EboksService;
import dk.digitalidentity.emailService.config.settings.EmailConfig;
import dk.digitalidentity.emailService.dao.model.Attachment;
import dk.digitalidentity.emailService.dao.model.MailType;
import dk.digitalidentity.emailService.service.EmailQueueService;
import dk.digitalidentity.emailService.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {
	private final EmailService emailService;
	private final EboksService eboksService;
	private final EmailQueueService emailQueueService;
	private final EmailConfig emailConfig;
	private final AttachmentService attachmentService;

	public void sendNotification(Employee employee, RenderedEmail email) {
		log.info("sendNotification called for employee={}, subject='{}'", employee.getId(), email.subject());
		if (employee.getCpr() != null) {
			log.info("Routing to eBoks for employee={} (CPR present)", employee.getId());

			// e-boks fetches the attachment from S3 when the message is sent, so only the key goes on the queue
			boolean queued = eboksService.addMessageToQueue(new EboksService.EboksMessageRequest(email.subject(), email.content(), employee.getCpr(), email.attachmentKey()));
			log.info("eBoks queue result for employee={}: {}", employee.getId(), queued);
			return;
		}
		else if (employee.getEmail() != null) {
			log.info("Routing to email for employee={} (no CPR, email present)", employee.getId());
			queueEmail(employee.getEmail(), email);
			return;
		}
		log.warn("No form of communication enabled for employee: {}", employee.getName());
	}

	public void sendNotificationUsingMail(String mail, RenderedEmail email) {
		if (StringUtils.hasText(mail) && StringUtils.hasText(email.subject()) && StringUtils.hasText(email.content())) {
			log.info("sendNotificationUsingMail called for mail={}, subject='{}'", mail, email.subject());
			queueEmail(mail, email);
		}
	}

	/**
	 * Sends the mail right away instead of putting it on the queue, so the caller does not have to wait for the next queue run.
	 * Returns true if the mail was handed over to the mail server.
	 */
	public boolean sendMailImmediately(String mail, RenderedEmail email) {
		if (!emailConfig.isEnabled()) {
			log.warn("Email is disabled - not sending mail to {}, subject='{}'", mail, email.subject());
			return false;
		}

		MailType mailType = emailService.resolveMailType();
		log.info("sendMailImmediately called for mail={}, subject='{}', mailType={}", mail, email.subject(), mailType);
		try {
			List<Attachment> attachments = attachmentService.load(email.attachmentKey()).map(FileAttachment::toMailAttachment).stream().toList();

			// the graph sender iterates cc/bcc without null checks, so pass empty lists
			return switch (mailType) {
				case GRAPH -> emailQueueService.sendMessageGraph(List.of(mail), List.of(), List.of(), email.subject(), email.content(), attachments, List.of());
				case SMTP -> emailQueueService.sendMessageSMTP(List.of(mail), List.of(), List.of(), email.subject(), email.content(), attachments, List.of());
				case null, default -> {
					log.warn("Cannot send mail immediately using mail type {} - mail to {} was not sent", mailType, mail);
					yield false;
				}
			};
		}
		catch (Exception e) {
			log.error("Failed to send mail immediately to {}, subject='{}'", mail, email.subject(), e);
			return false;
		}
	}

	private void queueEmail(String mail, RenderedEmail email) {
		List<Attachment> attachments = loadAttachment(email.attachmentKey()).map(FileAttachment::toMailAttachment).stream().toList();

		// the email library saves the attachments together with the queued mail, and deletes them when it is sent
		emailService.queueEmail(mail, email.subject(), email.content(), attachments);
	}

	// a queued mail cannot be retried later, so if S3 fails the mail is still sent - just without the attachment
	private Optional<FileAttachment> loadAttachment(String attachmentKey) {
		try {
			return attachmentService.load(attachmentKey);
		}
		catch (Exception e) {
			log.error("Could not load attachment {} from S3 - the mail is sent without it", attachmentKey, e);
			return Optional.empty();
		}
	}
}
