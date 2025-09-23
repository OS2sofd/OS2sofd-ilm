package dk.digitalidentity.OS2ilm.config.settings;

import dk.digitalidentity.OS2ilm.config.settings.modules.APIModule;
import dk.digitalidentity.OS2ilm.config.settings.modules.AdConfiguration;
import dk.digitalidentity.OS2ilm.config.settings.modules.Customer;
import dk.digitalidentity.OS2ilm.config.settings.modules.EBoks;
import dk.digitalidentity.OS2ilm.config.settings.modules.IdentityProviders;
import dk.digitalidentity.OS2ilm.config.settings.modules.Integration;
import dk.digitalidentity.OS2ilm.config.settings.modules.RoleCatalogueConfiguration;
import dk.digitalidentity.OS2ilm.config.settings.modules.ScheduledConfiguration;
import dk.digitalidentity.OS2ilm.config.settings.modules.SofdConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "os2ilm")
public class IlmConfiguration {
	private ScheduledConfiguration scheduled = new ScheduledConfiguration();
	private APIModule api = new APIModule();
    private EBoks eboks = new EBoks();
    private IdentityProviders idps = new IdentityProviders();
    private Customer customer = new Customer();
	private SofdConfiguration sofd = new SofdConfiguration();
	private RoleCatalogueConfiguration rc = new RoleCatalogueConfiguration();
	@NestedConfigurationProperty
	private Integration integrations = new Integration();
	private AdConfiguration activeDirectoryConfig = new AdConfiguration();
}
