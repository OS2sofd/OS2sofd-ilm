package dk.digitalidentity.OS2ilm.security;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.samlmodule.model.IdentityProvider;
import dk.digitalidentity.samlmodule.model.SamlIdentityProviderProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class IdentityProviderProvider implements SamlIdentityProviderProvider{
	private final IlmConfiguration configuration;

		// cached list of configured Identity Providers
	private List<IdentityProvider> identityProviders = new ArrayList<>();

	@Override
	public List<IdentityProvider> getIdentityProviders() {
		if (!identityProviders.isEmpty()) {
			return identityProviders;
		}

		IdentityProviderWithName employeeIdP = new IdentityProviderWithName();
		employeeIdP.setCvr(configuration.getCustomer().getCvr());
		employeeIdP.setEntityId(configuration.getIdps().getEmployeeIdPEntityId());
		employeeIdP.setMetadata(configuration.getIdps().getEmployeeIdPMetadata());
		employeeIdP.setName("Medarbejder Login");

		IdentityProviderWithName mitIdIdP = new IdentityProviderWithName();
		mitIdIdP.setCvr(null);
		mitIdIdP.setEntityId(configuration.getIdps().getMitIdIdPEntityId());
		mitIdIdP.setMetadata(configuration.getIdps().getMitIdIdPMetadata());
		mitIdIdP.setName("MitID Login");
		identityProviders.add(employeeIdP);
		identityProviders.add(mitIdIdP);
		return identityProviders;
	}

	@Override
	public IdentityProvider getByEntityId(String entityId) {
		for (IdentityProvider identityProvider : identityProviders) {
			if (Objects.equals(entityId, identityProvider.getEntityId())) {
				return identityProvider;
			}
		}
		return null;
	}
}
