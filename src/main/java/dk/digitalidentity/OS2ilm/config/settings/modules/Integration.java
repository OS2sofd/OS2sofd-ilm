package dk.digitalidentity.OS2ilm.config.settings.modules;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Getter
@Setter
public class Integration {
	@NestedConfigurationProperty
	private Cvr cvr = new Cvr();
}
