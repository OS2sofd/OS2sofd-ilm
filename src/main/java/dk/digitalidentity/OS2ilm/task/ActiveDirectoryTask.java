package dk.digitalidentity.OS2ilm.task;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.service.ActiveDirectorySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveDirectoryTask {
	private final IlmConfiguration configuration;
	private final ActiveDirectorySyncService activeDirectorySyncService;

	@Scheduled(cron = "${os2ilm.scheduled.updateActiveDirectoryCron:0 #{new java.util.Random().nextInt(55)} 6-20 * * *}")  // Every hour (at *:30) from 6-20 a clock
	public void updateActiveDirectory() {
		if (configuration.getScheduled().isEnabled() && configuration.getScheduled().isUpdateActiveDirectoryEnabled()) {
			log.info("Synchronizing active directory groups...");

			activeDirectorySyncService.syncGroups();

			log.info("Finished synchronizing active directory groups.");
		}
	}

	@Scheduled(cron = "${os2ilm.scheduled.accountActiveDirectoryCron:0 #{new java.util.Random().nextInt(55)} 6-20 * * *}")  // Every hour (at *:30) from 6-20 a clock
	public void accountAcivationAndDeactivation() {
		if (configuration.getScheduled().isEnabled() && configuration.getScheduled().isAccountActiveDirectoryEnabled()) {
			log.info("Synchronizing active directory groups...");

			activeDirectorySyncService.checkForActivationsAndDeactivations();

			log.info("Finished synchronizing active directory groups.");
		}
	}

}
