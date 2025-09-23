package dk.digitalidentity.OS2ilm.config.settings.modules.api;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalModule {
	private boolean enabled = true;
	private String apiKey;
}
