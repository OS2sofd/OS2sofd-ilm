package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.exception.ADOperationException;
import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.dto.NamesDTO;
import dk.digitalidentity.OS2ilm.model.exception.InvalidAccountNameException;
import dk.digitalidentity.OS2ilm.util.NameUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AccountNameTemplateService {

	private static final String SEQUENCE_TOKEN = "løbenummer";
	private static final List<String> KNOWN_TOKENS = List.of(
			SEQUENCE_TOKEN,
			"person-fornavn",
			"person-efternavn",
			"person-navn",
			"person-initialer",
			"virksomhed-navn",
			"virksomhed-initialer"
	);
	private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{([^{}]+)}");

	private final AdUserService adUserService;

	public String resolve(String template, Employee employee) {
		validateTemplateSyntax(template);

		NamesDTO names = NameUtils.splitName(employee);
		String companyName = employee.getBusiness().getName();

		Map<String, String> staticValues = Map.of(
				"person-fornavn", names.firstName(),
				"person-efternavn", names.lastName(),
				"person-navn", employee.getName() != null ? employee.getName() : "",
				"person-initialer", NameUtils.getPersonInitials(employee),
				"virksomhed-navn", companyName != null ? companyName : "",
				"virksomhed-initialer", companyName != null ? NameUtils.getCompanyInitials(companyName) : ""
		);

		String resolvedStaticPart = substitute(template, staticValues);

		if (!template.contains("{" + SEQUENCE_TOKEN + "}")) {
			return resolvedStaticPart;
		}

		// {løbenummer} is always the final token in the template (enforced by validateTemplateSyntax),
		// so the text before it is a stable prefix we can scan existing AD usernames against.
		String prefix = resolvedStaticPart.substring(0, resolvedStaticPart.length() - ("{" + SEQUENCE_TOKEN + "}").length());

		List<AdUser> existingUsers = adUserService.findByNameStartingWith(prefix);
		int maxNumber = existingUsers.stream()
				.map(AdUser::getName)
				.map(username -> username.substring(prefix.length()))
				.filter(suffix -> suffix.matches("\\d+"))
				.mapToInt(suffix -> {
					try {
						return Integer.parseInt(suffix);
					}
					catch (NumberFormatException e) {
						throw new ADOperationException("Kunne ikke fortolke løbenummer for eksisterende AD-konto: " + suffix, e);
					}
				})
				.max()
				.orElse(0);

		return prefix + String.format("%04d", maxNumber + 1);
	}

	public void validateTemplateSyntax(String template) {
		if (!StringUtils.hasText(template)) {
			throw new InvalidAccountNameException(template);
		}

		Matcher matcher = TOKEN_PATTERN.matcher(template);
		int sequenceTokenCount = 0;
		int sequenceTokenEnd = -1;
		while (matcher.find()) {
			String token = matcher.group(1);
			if (!KNOWN_TOKENS.contains(token)) {
				throw new InvalidAccountNameException(template);
			}
			if (token.equals(SEQUENCE_TOKEN)) {
				sequenceTokenCount++;
				sequenceTokenEnd = matcher.end();
			}
		}

		if ((sequenceTokenEnd != -1 && sequenceTokenEnd != template.length()) || sequenceTokenCount > 1) {
			throw new InvalidAccountNameException(template);
		}

		String withoutTokens = TOKEN_PATTERN.matcher(template).replaceAll("");
		if (withoutTokens.indexOf('{') != -1 || withoutTokens.indexOf('}') != -1) {
			throw new InvalidAccountNameException(template);
		}
	}

	private String substitute(String template, Map<String, String> values) {
		String result = template;
		for (Map.Entry<String, String> entry : values.entrySet()) {
			result = result.replace("{" + entry.getKey() + "}", entry.getValue());
		}
		return result;
	}
}
