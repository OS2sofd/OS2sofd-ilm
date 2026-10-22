package dk.digitalidentity.OS2ilm.config.settings.modules;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Getter
@Setter
public class AdConfiguration {
	@NestedConfigurationProperty
	private String prefix = "ILM-PLACEHOLDER_BUSINESS_PREFIX-";
	private int passwordLength = 16;
	private String cprAttribute = "employeeNumber";
	private String phoneAttribute = "mobile";
	private String companyAttribute = "company";
	private String targetOu;
	private String rootGroup;
	private String upnDomain;
	private boolean cprWithDash = false;
}
