package dk.digitalidentity.OS2ilm.config.settings.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Customer {
	private String cvr;
	private String name;
	private String generalEmail;
	private boolean requirePhone = false;
	private boolean requireRegistrationReason = false;
	private boolean lockNameFromMitId = false;
	private String s3FileBasePrefix;
}
