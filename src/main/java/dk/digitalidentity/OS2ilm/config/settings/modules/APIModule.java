package dk.digitalidentity.OS2ilm.config.settings.modules;

import dk.digitalidentity.OS2ilm.config.settings.modules.api.BusinessModule;
import dk.digitalidentity.OS2ilm.config.settings.modules.api.EmployeeModule;
import dk.digitalidentity.OS2ilm.config.settings.modules.api.InternalModule;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class APIModule {
	private InternalModule internal = new InternalModule();
	private BusinessModule business = new BusinessModule();
	private EmployeeModule employee = new EmployeeModule();
}
