package dk.digitalidentity.OS2ilm.security;

import dk.digitalidentity.samlmodule.model.SamlLoginPostProcessor;
import dk.digitalidentity.samlmodule.model.TokenUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Transactional
@RequiredArgsConstructor
public class LoginPostProcessor implements SamlLoginPostProcessor {
	private final SecurityUtil securityUtil;

	@Override
	public void process(TokenUser tokenUser) {
		securityUtil.updateTokenUser(tokenUser);
	}
}
