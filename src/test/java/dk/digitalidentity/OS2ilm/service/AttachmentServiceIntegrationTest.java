package dk.digitalidentity.OS2ilm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import dk.digitalidentity.OS2ilm.TestContainerConfiguration;
import dk.digitalidentity.OS2ilm.service.EmailTemplateService.RenderedEmail;
import dk.digitalidentity.emailService.dao.EmailQueueDao;
import dk.digitalidentity.emailService.dao.model.InlineImage;
import dk.digitalidentity.emailService.service.EmailService;
import dk.digitalidentity.framework.s3fileshare.service.S3Service;

/**
 * Runs against a real database, because the point is to verify that the email library saves attachments with the queued mail and deletes them again
 */
@SpringBootTest(properties = {
		// never send the queued test mails
		"fw.email.service.enabled=false",
		"fw.email.service.send.cron=-",
		"os2ilm.scheduled.enabled=false"
})
@Import(TestContainerConfiguration.class)
class AttachmentServiceIntegrationTest {
	private static final String RECIPIENT = "test@example.invalid";
	private static final String ATTACHMENT_KEY = "kommune/emailtemplate/vejledning.pdf";

	@Autowired private MessageService messageService;
	@Autowired private EmailService emailService;
	@Autowired private EmailQueueDao emailQueueDao;
	@Autowired private JdbcTemplate jdbc;

	@MockitoBean private S3Service s3Service;

	@BeforeEach
	void clearQueue() {
		jdbc.update("DELETE FROM fw_email_queue_attachment");
		jdbc.update("DELETE FROM fw_email_queue_attachment_file");
		jdbc.update("DELETE FROM fw_email_queue_inline_image");
		jdbc.update("DELETE FROM fw_email_queue");
	}

	@Test
	void queuesTheEmailWithTheAttachmentFromS3() {
		when(s3Service.readFile(ATTACHMENT_KEY)).thenReturn(out -> out.write("pdf-indhold".getBytes()));

		messageService.sendNotificationUsingMail(RECIPIENT, new RenderedEmail("Emne", "<p>Besked</p>", ATTACHMENT_KEY));

		Long queueId = jdbc.queryForObject("SELECT id FROM fw_email_queue", Long.class);
		assertThat(jdbc.queryForObject("SELECT email_queue_id FROM fw_email_queue_attachment WHERE file_name = 'vejledning.pdf'", Long.class)).isEqualTo(queueId);
		assertThat(jdbc.queryForObject("SELECT f.content FROM fw_email_queue_attachment_file f JOIN fw_email_queue_attachment a ON a.attachment_file_id = f.id", byte[].class))
				.isEqualTo("pdf-indhold".getBytes());
	}

	@Test
	void queuesTheEmailWithoutAttachmentWhenS3Fails() {
		when(s3Service.readFile(ATTACHMENT_KEY)).thenThrow(new RuntimeException("S3 nede"));

		messageService.sendNotificationUsingMail(RECIPIENT, new RenderedEmail("Emne", "<p>Besked</p>", ATTACHMENT_KEY));

		assertThat(count("fw_email_queue")).isEqualTo(1);
		assertThat(count("fw_email_queue_attachment")).isZero();
	}

	@Test
	void deletingASentEmailLeavesNothingBehind() {
		when(s3Service.readFile(ATTACHMENT_KEY)).thenReturn(out -> out.write("pdf-indhold".getBytes()));
		messageService.sendNotificationUsingMail(RECIPIENT, new RenderedEmail("Emne", "<p>Besked</p>", ATTACHMENT_KEY));

		// this is what the email library does after a mail is sent
		emailQueueDao.deleteAll(emailQueueDao.findAll());

		assertThat(count("fw_email_queue_attachment")).isZero();
		assertThat(count("fw_email_queue_attachment_file")).isZero();
	}

	@Test
	void storesLargeMessagesAndInlineImages() {
		// more than 64 KB once encoded as UTF-8, and a data uri far beyond the old 255 character limit
		String message = "<p>" + "æøå".repeat(15_000) + "</p>";
		String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(new byte[10_000]);

		emailService.queueEmail(List.of(RECIPIENT), null, null, "Emne", message, List.of(), List.of(InlineImage.ofDataUri("logo", dataUri)));

		assertThat(jdbc.queryForObject("SELECT message FROM fw_email_queue", String.class)).isEqualTo(message);
		assertThat(jdbc.queryForObject("SELECT src FROM fw_email_queue_inline_image", String.class)).isEqualTo(dataUri);

		emailQueueDao.deleteAll(emailQueueDao.findAll());
		assertThat(count("fw_email_queue_inline_image")).isZero();
	}

	private long count(String table) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
	}
}
