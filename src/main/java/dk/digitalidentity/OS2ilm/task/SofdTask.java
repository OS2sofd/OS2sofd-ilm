package dk.digitalidentity.OS2ilm.task;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.enums.AccountOrderResult;
import dk.digitalidentity.OS2ilm.model.entity.enums.ExchangeAccountStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.SofdSyncStatus;
import dk.digitalidentity.OS2ilm.service.AdUserService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.SofdQueueService;
import dk.digitalidentity.OS2ilm.service.SofdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SofdTask {
	private final IlmConfiguration configuration;
	private final SofdService sofdService;
	private final AdUserService adUserService;
	private final SofdQueueService sofdQueueService;
	private final EmployeeService employeeService;

	// Every hour at a random minute
	@Scheduled(cron = "${os2ilm.scheduled.sofdSyncCron:0 #{new java.util.Random().nextInt(60)} * * * *}")
	public void syncOrgUnits() {
		if (configuration.getScheduled().isEnabled() && configuration.getSofd().isEnabled()) {
			log.info("Starting Sofd Sync");

			sofdService.syncData();

			log.info("Finished Sofd Sync");
		}
	}

	// Every third minute with a random offset
	@Scheduled(cron = "${os2ilm.scheduled.sofdEmployeeIdSyncCron:0 #{new java.util.Random().nextInt(3)}/3 * * * *}")
	public void syncEmployeeIds() {
		if (!configuration.getScheduled().isEnabled() || !configuration.getSofd().isEnabled()) {
			return;
		}

		List<AdUser> pending = adUserService.findBySofdSyncStatus(SofdSyncStatus.CREATED);
		if (pending.isEmpty()) {
			return;
		}

		log.info("Syncing SOFD employeeId for {} AdUser(s)", pending.size());

		int synced = 0, pendingRetry = 0;
		for (AdUser adUser : pending) {
			try {
				if (sofdQueueService.patchEmployeeIdForAdUser(adUser)) {
					adUser.setSofdSyncStatus(SofdSyncStatus.SYNCED);
					adUserService.save(adUser);
					synced++;
				} else {
					pendingRetry++;
				}
			}
			catch (Exception ex) {
				log.warn("Failed to sync employeeId for adUser {}: {}", adUser.getName(), ex.getMessage(), ex);
			}
		}

		log.info("SOFD employeeId sync: synced={}, pending_retry={}, total={}", synced, pendingRetry, pending.size());
	}

	// Every fifth minute with a random offset
	@Scheduled(cron = "${os2ilm.scheduled.sofdExchangeSyncCron:0 #{new java.util.Random().nextInt(5)}/5 * * * *}")
	public void orderExchangeAccounts() {
		if (!configuration.getScheduled().isEnabled() || !configuration.getSofd().isEnabled()) {
			return;
		}

		List<Employee> needingOrder = employeeService.findByExchangeAccountStatus(ExchangeAccountStatus.NEEDS_ORDER);
		if (needingOrder.isEmpty()) {
			return;
		}

		int ordered = 0, permanentFailures = 0, pendingRetry = 0, notReady = 0;
		for (Employee employee : needingOrder) {
			try {
				AdUser adUser = employee.getUser();
				// Only order once the AD account exists, is enabled, and its employeeId is synced to SOFD.
				// Otherwise leave the employee as NEEDS_ORDER so a later run retries when preconditions are met.
				if (adUser == null || !adUser.isEnabled() || adUser.getSofdSyncStatus() != SofdSyncStatus.SYNCED) {
					notReady++;
					continue;
				}
				AccountOrderResult result = sofdService.createExchangeAccount(employee.getExtUuid(), adUser.getName(), employee.getExtAffiliationUuid());

				sofdService.applyAccountOrderResult(employee, adUser, result);
				switch (result) {
					case ORDERED -> ordered++;
					case PERMANENT_FAILURE -> permanentFailures++;
					case TRANSIENT_FAILURE -> pendingRetry++;
				}
			}
			catch (Exception ex) {
				log.warn("Failed to order an exchange account for employee {}: {}", employee.getName(), ex.getMessage(), ex);
			}
		}

		log.info("SOFD accountOrder creation: ordered={}, pending_retry={}, permanent_failures={}, not_ready={}, total={}", ordered, pendingRetry, permanentFailures, notReady, needingOrder.size());
	}
}
