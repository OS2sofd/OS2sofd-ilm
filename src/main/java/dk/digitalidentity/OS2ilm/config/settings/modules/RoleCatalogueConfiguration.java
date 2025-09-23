package dk.digitalidentity.OS2ilm.config.settings.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleCatalogueConfiguration {
	private String url;
	private String apiKey;
	private boolean enabled = true;
	
	private boolean enabledForWorkplaces = false;
	private boolean allowAutomaticUserRoles = false;
}
