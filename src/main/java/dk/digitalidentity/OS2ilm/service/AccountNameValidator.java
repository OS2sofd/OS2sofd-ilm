package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.exception.InvalidAccountNameException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Service
public class AccountNameValidator {

	private static final int MAX_LENGTH = 20;
	private static final Pattern DISALLOWED_CHARACTERS = Pattern.compile("[\"/\\\\\\[\\]:;|=,+*?<>@]");

	public void validate(String candidateName) {
		if (!StringUtils.hasText(candidateName)) {
			throw new InvalidAccountNameException(candidateName);
		}

		if (candidateName.length() > MAX_LENGTH) {
			throw new InvalidAccountNameException(candidateName);
		}

		if (candidateName.startsWith(".") || candidateName.endsWith(".")) {
			throw new InvalidAccountNameException(candidateName);
		}

		if (DISALLOWED_CHARACTERS.matcher(candidateName).find()) {
			throw new InvalidAccountNameException(candidateName);
		}
	}
}
