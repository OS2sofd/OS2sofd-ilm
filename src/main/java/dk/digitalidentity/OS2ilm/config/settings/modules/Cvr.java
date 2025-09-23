package dk.digitalidentity.OS2ilm.config.settings.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cvr {
	private boolean enabled = true;
	private String apiKey = "";
	private String baseUrl = "https://datafordeler.digital-identity.dk/proxy";
}
