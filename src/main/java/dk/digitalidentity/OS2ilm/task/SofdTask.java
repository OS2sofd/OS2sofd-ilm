package dk.digitalidentity.OS2ilm.task;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.service.SofdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SofdTask {
	private final IlmConfiguration configuration;
	private final SofdService sofdService;

	// handful times a day
	@Scheduled(cron = "${os2ilm.scheduled.sofdSyncCron:0 0 * * * *}")
	public void syncOrgUnits() {
		if (configuration.getScheduled().isEnabled() && configuration.getSofd().isEnabled()) {
			log.info("Starting Sofd Sync");

			sofdService.syncData();

			log.info("Finished Sofd Sync");
		}
	}
}
