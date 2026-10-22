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
public class SendApprovalEmailNotificationsTask {

	private final IlmConfiguration configuration;
	private final EmployeeService employeeService;

	@Scheduled(cron = "${os2ilm.scheduled.sendAwaitingApprovalEmailNotificationsCron:0 0 8 * * ?}")
	public void sendAwaitingApprovalEmailNotifications () {
		if (configuration.getScheduled().isEnabled() && configuration.getScheduled().isSendAwaitingApprovalEmailNotificationsEnabled()) {
			log.info("Running task: SendAwaitingApprovalEmailNotifications");

			employeeService.sendAwaitingApprovalNotifications();

			log.info("Finished task: SendAwaitingApprovalEmailNotifications");
		}
	}

}
