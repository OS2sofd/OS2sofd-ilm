package dk.digitalidentity.OS2ilm.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.dao.EmailTemplateAttachmentDeletionDao;
import dk.digitalidentity.OS2ilm.model.dao.EmailTemplateDao;
import dk.digitalidentity.OS2ilm.model.entity.EmailTemplateAttachmentDeletion;
import dk.digitalidentity.emailService.dao.model.Attachment;
import dk.digitalidentity.framework.s3fileshare.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Email template attachments live in the customer's S3 bucket and are fetched when a message is sent (e-boks) or queued (email)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {
	// queued e-boks messages only store the S3 key and read the file when they are sent, so a replaced file is kept long enough for those to be sent
	public static final Duration ATTACHMENT_DELETION_DELAY = Duration.ofDays(7);

	// uploaded files are stored as <uuid>-<filename>, and the recipient should only see the filename
	private static final Pattern UPLOAD_PREFIX = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}-");

	private final S3Service s3Service;
	private final EmailTemplateDao emailTemplateDao;
	private final EmailTemplateAttachmentDeletionDao emailTemplateAttachmentDeletionDao;
	private final IlmConfiguration configuration;

	public record FileAttachment(String filename, byte[] content) {

		// the email library's attachment, which is saved together with the queued mail
		public Attachment toMailAttachment() {
			return Attachment.of(filename, content);
		}
	}

	/**
	 * Reads the attachment from S3. Returns empty if no key is given, and throws if the file could not be read.
	 */
	public Optional<FileAttachment> load(String s3Key) {
		if (!StringUtils.hasText(s3Key)) {
			return Optional.empty();
		}

		ByteArrayOutputStream content = new ByteArrayOutputStream();
		try {
			s3Service.readFile(s3Key).writeTo(content);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not read attachment " + s3Key + " from S3", ex);
		}

		String filename = UPLOAD_PREFIX.matcher(getFilename(s3Key)).replaceFirst("");

		return Optional.of(new FileAttachment(filename, content.toByteArray()));
	}

	/**
	 * Uploads the file to S3 as the template's attachment, under a fresh key so it cannot collide with (or overwrite) a previous attachment.
	 * The caller is responsible for scheduling the previous key, if any, for deletion once the template is saved with the new one.
	 */
	public String uploadEmailTemplateAttachment(MultipartFile file) {
		String key = configuration.getCustomer().getS3FileBasePrefix() + "/emailtemplate/" + UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());

		try {
			s3Service.uploadFile(key, file.getInputStream(), file.getSize(), true);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not upload attachment " + key + " to S3", ex);
		}

		return key;
	}

	/**
	 * Marks a (possibly null) email template attachment for deletion from S3 once ATTACHMENT_DELETION_DELAY has passed
	 */
	public void scheduleEmailTemplateAttachmentDeletion(String s3Key) {
		if (!StringUtils.hasText(s3Key)) {
			return;
		}

		EmailTemplateAttachmentDeletion deletion = new EmailTemplateAttachmentDeletion();
		deletion.setS3Key(s3Key);
		deletion.setDeleteAfter(LocalDateTime.now().plus(ATTACHMENT_DELETION_DELAY));
		emailTemplateAttachmentDeletionDao.save(deletion);
	}

	/**
	 * Deletes the attachments whose deletion delay has passed. A file that could not be deleted is tried again on the next run.
	 */
	public void deleteExpiredEmailTemplateAttachments() {
		List<EmailTemplateAttachmentDeletion> expired = emailTemplateAttachmentDeletionDao.findByDeleteAfterBefore(LocalDateTime.now());

		for (EmailTemplateAttachmentDeletion deletion : expired) {
			// keys are unique per upload, so this is only a safety net against deleting a file that a template uses
			if (!emailTemplateDao.existsByS3AttachmentKey(deletion.getS3Key())) {
				try {
					s3Service.deleteFile(deletion.getS3Key());
				}
				catch (Exception ex) {
					log.error("Could not delete attachment {} from S3", deletion.getS3Key(), ex);
					continue;
				}
			}

			emailTemplateAttachmentDeletionDao.delete(deletion);
		}

		if (!expired.isEmpty()) {
			log.info("Processed {} email template attachment(s) scheduled for deletion", expired.size());
		}
	}

	// strips any path segments and characters that could otherwise be used to construct an unexpected S3 key
	private static String sanitizeFilename(String filename) {
		String name = StringUtils.hasText(filename) ? getFilename(filename.replace('\\', '/')) : "fil";
		String sanitized = name.replaceAll("[^A-Za-z0-9æøåÆØÅ._-]", "_");

		return StringUtils.hasText(sanitized) ? sanitized : "fil";
	}

	private static String getFilename(String s3Key) {
		return s3Key.substring(s3Key.lastIndexOf('/') + 1);
	}
}
