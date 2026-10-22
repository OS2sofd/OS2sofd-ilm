package dk.digitalidentity.OS2ilm.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanUpEmailAttachmentsTask {
	private final IlmConfiguration configuration;
	private final AttachmentService attachmentService;

	@Scheduled(cron = "${os2ilm.scheduled.cleanUpEmailAttachmentsCron:0 #{new java.util.Random().nextInt(55)} * * * ?}") // every hour
	public void cleanUpEmailAttachments() {
		if (configuration.getScheduled().isEnabled()) {
			log.info("Running task: CleanUpEmailAttachments");

			attachmentService.deleteExpiredEmailTemplateAttachments();

			log.info("Finished task: CleanUpEmailAttachments");
		}
	}
}
