package dk.digitalidentity.OS2ilm.config.settings.modules;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EBoks {
	private String url = "http://sf1601.digital-identity.dk/api/print";

	private boolean enabled = true;
	private String senderName;
	private String cvr;
}
