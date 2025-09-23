package dk.digitalidentity.OS2ilm.task;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanUpEmployeeTask {
	private final IlmConfiguration configuration;
	private final EmployeeService employeeService;

	@Scheduled(cron = "${os2ilm.scheduled.cleanUpOldUsersCron:0 0 0 * * ?}")
	public void cleanUpOldUsers () {
		if (configuration.getScheduled().isEnabled() && configuration.getScheduled().isCleanUpOldUsersEnabled()) {
			log.info("Running task: CleanUpOldUsers");

			employeeService.cleanUpOldUsers();

			log.info("Finished task: CleanUpOldUsers");
		}
	}

}
