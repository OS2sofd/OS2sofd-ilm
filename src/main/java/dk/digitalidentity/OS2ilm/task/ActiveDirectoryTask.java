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

	// High frequency during morning activation window (6:00-9:00)
	@Scheduled(cron = "${os2ilm.scheduled.morningHighFrequencyAccountMaintenanceCron: #{new java.util.Random().nextInt(55)} 0/5 6-9 * * MON-FRI}") // Every 5 minutes, 6-8 AM, weekdays
	public void checkActivationsPeakMorning() {
		if (configuration.getScheduled().isEnabled() && configuration.getScheduled().isAccountActiveDirectoryEnabled()) {
			log.info("Started enabling and disabling users during morning peak hours...");

			activeDirectorySyncService.checkForActivationsAndDeactivations();

			log.info("Finished enabling and disabling users during morning peak hours...");
		}
	}

	// High frequency during afternoon deactivation window (14:00-17:00)
	@Scheduled(cron = "${os2ilm.scheduled.afternoonHighFrequencyAccountMaintenanceCron:#{new java.util.Random().nextInt(55)} 0/5 14-17 * * MON-FRI}") // Every 5 minutes, 2-5 PM, weekdays
	public void checkActivationsPeakAfternoon() {
		if (configuration.getScheduled().isEnabled() && configuration.getScheduled().isAccountActiveDirectoryEnabled()) {
			log.info("Started enabling and disabling users during afternoon peak hours...");

			activeDirectorySyncService.checkForActivationsAndDeactivations();

			log.info("Finished enabling and disabling users during afternoon peak hours...");
		}
	}

	// Lower frequency for off-peak hours
	@Scheduled(cron = "${os2ilm.scheduled.weekLowFrequencyAccountMaintenanceCron:#{new java.util.Random().nextInt(55)} 0/15 9-14,17-20 * * MON-FRI}") // Every 15 minutes, off-peak weekdays
	@Scheduled(cron = "${os2ilm.scheduled.weekendLowFrequencyAccountMaintenanceCron:#{new java.util.Random().nextInt(55)} 0/15 * * * SAT-SUN}") // Every 15 minutes, weekends
	public void checkActivationsOffPeak() {
		if (configuration.getScheduled().isEnabled() && configuration.getScheduled().isAccountActiveDirectoryEnabled()) {
			log.info("Started enabling and disabling users during off-peak and weekend hours...");

			activeDirectorySyncService.checkForActivationsAndDeactivations();

			log.info("Finished enabling and disabling users during off-peak and weekend hours...");
		}
	}

}
