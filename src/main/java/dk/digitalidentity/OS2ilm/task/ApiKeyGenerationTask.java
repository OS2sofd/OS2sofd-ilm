package dk.digitalidentity.OS2ilm.task;

import java.util.List;
import java.util.UUID;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.entity.Client;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApiRole;
import dk.digitalidentity.OS2ilm.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ApiKeyGenerationTask {
	private final IlmConfiguration configuration;
	private final ClientService clientService;

	@EventListener(ApplicationReadyEvent.class)
	public void runOnStartup() {
		if (!configuration.getScheduled().isEnabled() && !configuration.getScheduled().isApiKeyGenerationEnabled()) {
			return;
		}
		
		List<Client> existingClients = clientService.findAll();
		
		for (ApiRole apiRole : ApiRole.values()) {
			Client client = existingClients.stream().filter(c -> c.getRole().equals(apiRole)).findFirst().orElse(null);

			if (client == null) {
				// migrate logic - can be removed once all customers have been migrated, and then we can remove
				// the keys from the configuration files and configuration classes
				String apiKey = null;
				String description = null;
				switch (apiRole) {
					case INTERNAL:
						apiKey = configuration.getApi().getInternal().getApiKey();
						description = "API til at indlæse intern data, som f.eks demodata";
						break;
					case BUSINESS:
						apiKey = configuration.getApi().getBusiness().getApiKey();
						description = "API til at indlæse/udlæse virksomheder";
						break;
					case EMPLOYEE:
						apiKey = configuration.getApi().getEmployee().getApiKey();
						description = "API til at indlæse/udlæse medarbejdere";
				}
				
				if (!StringUtils.hasLength(apiKey)) {
					apiKey = UUID.randomUUID().toString();
				}
				
				client = new Client();
				client.setApiKey(apiKey);
				client.setDescription(description);
				client.setEnabled(true);
				client.setRole(apiRole);
				
				clientService.save(client);
			}
		}
	}
}
