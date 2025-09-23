package dk.digitalidentity.OS2ilm.security;

import dk.digitalidentity.OS2ilm.model.role.NoRoleRequired;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@NoRoleRequired
@RequiredArgsConstructor
public class IdpDiscoveryController {
	private final IdentityProviderProvider identityProviderProvider;

	@GetMapping("/discovery")
	public String discovery(Model model) {
		model.addAttribute("identityProviders", identityProviderProvider.getIdentityProviders());
		return "discovery";
	}
}
