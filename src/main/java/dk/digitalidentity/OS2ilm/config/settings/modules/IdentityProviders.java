package dk.digitalidentity.OS2ilm.config.settings.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdentityProviders {

	// employee login
	private String employeeIdPEntityId = "https://saml.n2adgangsstyring.stoettesystemerne.dk/runtime";
	private String employeeIdPMetadata = "https://n2adgangsstyring.stoettesystemerne.dk/runtime/saml2/metadata.idp";

	// employee login role constants
	private String employeeRoleSystemAdministrator = "https://os2ilm.dk/roles/usersystemrole/sysadmin/1";
	private String employeeRoleConsultantAdministrator = "https://os2ilm.dk/roles/usersystemrole/consultantadmin/1";
	private String employeeRoleConsultant = "https://os2ilm.dk/roles/usersystemrole/consultant/1";
	private String employeeRoleReadOnly = "https://os2ilm.dk/roles/usersystemrole/readonly/1";

	// MitID login
	private String mitIdIdPEntityId = "https://saml.nemlog-in.dk";
	private String mitIdIdPMetadata = "https://www.digital-identity.dk/metadata/nemlogin3-prod-metadata.xml";

	// constraints
	private String cvrConstraint = "http://os2ilm.dk/constraints/agency/1";
}
