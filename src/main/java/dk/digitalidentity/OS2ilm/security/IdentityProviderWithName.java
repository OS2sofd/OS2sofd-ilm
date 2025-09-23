package dk.digitalidentity.OS2ilm.security;

import dk.digitalidentity.samlmodule.model.IdentityProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Getter
@Setter
public class IdentityProviderWithName extends IdentityProvider {
	private String name;

	@Transient
	public String getLoginId() {
		return Base64.getEncoder().encodeToString(super.getEntityId().getBytes(StandardCharsets.UTF_8));
	}
}
