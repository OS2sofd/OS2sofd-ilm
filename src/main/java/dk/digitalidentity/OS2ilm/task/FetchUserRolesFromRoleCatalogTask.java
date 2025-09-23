package dk.digitalidentity.OS2ilm.task;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.service.RoleCatalogueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class FetchUserRolesFromRoleCatalogTask {

	private final RoleCatalogueService roleCatalogService;
	private final IlmConfiguration config;

	// a handfuld of times a day
    @Scheduled(cron = "${os2ilm.scheduled.rcSyncCron:0 0 * * * ?}")
	public synchronized void synchronizeUserRoles() {
		if (!config.getScheduled().isEnabled() || !config.getRc().isEnabled()) {
			return;
		}

		log.info("Running task: synchronizeUserRoles from RC");

		roleCatalogService.syncData();

		log.info("Finished task: synchronizeUserRoles from RC");
	}

	@Scheduled(cron = "${os2ilm.scheduled.rcAssignCron:0 0 * * * ?}") // Probably needs to happen more often
	public void assignRolesToUsers() {
		if (config.getScheduled().isEnabled() || config.getRc().isEnabled()) {
			log.info("Assigning roles to users in RC");

			roleCatalogService.assignRolesToUsersInRc();

			log.info("Finished assigning roles to users in RC");
		}
	}
}
