package dk.digitalidentity.OS2ilm.task;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.entity.ApplicationApiDTO;
import dk.digitalidentity.OS2ilm.service.AppManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckVersionTask {

	private final IlmConfiguration configuration;
	private final AppManagerService appManagerService;

	@Scheduled(initialDelay = 1000, fixedDelay = 60 * 60 * 1000)
	public void checkVersion() {
		log.debug("Checking application version");
		String appManagerUrl = configuration.getAppManager().getUrl();
		if (!StringUtils.hasLength(appManagerUrl)) {
			log.warn("No url set for version checking, skipping.");
			return;
		}

		List<ApplicationApiDTO> applications = appManagerService.getApplications(appManagerUrl);
		if (applications == null) {
			return;
		}

		for (ApplicationApiDTO app : applications) {
			if (Objects.equals(app.getIdentifier(), "os2ilm")) {
				configuration.setLatestVersion(app.getNewestVersion());
				log.info("Latest available version: {}", app.getNewestVersion());
			}
		}
	}
}
