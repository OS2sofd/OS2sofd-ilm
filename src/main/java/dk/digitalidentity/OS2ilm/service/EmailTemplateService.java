package dk.digitalidentity.OS2ilm.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.owasp.html.HtmlChangeListener;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.dao.EmailTemplateDao;
import dk.digitalidentity.OS2ilm.model.entity.EmailTemplate;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplateType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailTemplateService {
	public static final int MAX_SUBJECT_LENGTH = 255;
	public static final int MAX_CONTENT_LENGTH = 65535;

	public static final Duration TEST_EMAIL_INTERVAL = Duration.ofSeconds(15);

	// characters that are known to cause problems when sending via Digital Post
	private static final List<String> DIGITAL_POST_INVALID_SUBJECT_CHARACTERS = List.of(":");

	private static final PolicyFactory CONTENT_POLICY = new HtmlPolicyBuilder()
			.allowCommonBlockElements()
			.allowCommonInlineFormattingElements()
			.allowElements("a", "pre", "table", "thead", "tbody", "tfoot", "tr", "th", "td")
			.allowUrlProtocols("https", "mailto")
			.allowAttributes("href", "target").onElements("a")
			.allowAttributes("colspan", "rowspan").onElements("td", "th")
			.allowStyling()
			.allowAttributes("class", "id", "name").globally()
			.toFactory();

	private final EmailTemplateDao emailTemplateDao;
	private final MessageService messageService;
	private final IlmConfiguration configuration;

	// last time each user sent a test email, used to rate limit the test email endpoint
	private final Map<String, Instant> lastTestEmailSent = new ConcurrentHashMap<>();

	/**
	 * attachmentKey is the S3 key of the template's attachment, or null if it has none
	 */
	public record RenderedEmail(String subject, String content, String attachmentKey) {}

	public enum TestEmailResult { SENT, LOGGED, FAILED }

	/**
	 * The sanitized content, and a description of every tag and attribute the sanitizer removed (in the order they were found)
	 */
	public record SanitizedContent(String content, Set<String> removed) {
		public boolean hasRemoved() {
			return !removed.isEmpty();
		}
	}

	// collects what the sanitizer throws away, so the admin can be told instead of content silently disappearing
	private static final HtmlChangeListener<Set<String>> REMOVED_HTML_COLLECTOR = new HtmlChangeListener<>() {
		@Override
		public void discardedTag(Set<String> removed, String elementName) {
			removed.add("<" + elementName + ">");
		}

		@Override
		public void discardedAttributes(Set<String> removed, String tagName, String... attributeNames) {
			for (String attributeName : attributeNames) {
				removed.add(attributeName + " på <" + tagName + ">");
			}
		}
	};

	@Transactional
	public List<EmailTemplate> findAll() {
		return Arrays.stream(EmailTemplateType.values())
				.map(this::getOrCreate)
				.toList();
	}

	public EmailTemplate findById(long id) {
		return emailTemplateDao.findById(id).orElse(null);
	}

	@Transactional
	public EmailTemplate getByType(EmailTemplateType templateType) {
		return getOrCreate(templateType);
	}

	@Transactional
	public EmailTemplate save(EmailTemplate emailTemplate) {
		emailTemplate.setLastUpdated(LocalDateTime.now());
		return emailTemplateDao.save(emailTemplate);
	}

	/**
	 * Returns a danish error message suitable for showing to the user, or null if the values are valid
	 */
	public String validate(EmailTemplate emailTemplate, String subject, String content) {
		if (!StringUtils.hasText(subject)) {
			return "Emnefeltet skal udfyldes";
		}

		if (subject.length() > MAX_SUBJECT_LENGTH) {
			return "Emnefeltet må højst være " + MAX_SUBJECT_LENGTH + " tegn";
		}

		if (emailTemplate.getTemplateType().isDigitalPost()) {
			for (String invalidCharacter : DIGITAL_POST_INVALID_SUBJECT_CHARACTERS) {
				if (subject.contains(invalidCharacter)) {
					return "Emnefeltet må ikke indeholde '" + invalidCharacter + "', da beskeden kan blive sendt via Digital Post";
				}
			}
		}

		if (!StringUtils.hasText(content)) {
			return "Beskeden skal udfyldes";
		}

		if (content.length() > MAX_CONTENT_LENGTH) {
			return "Den indtastede tekst er for lang. Der tillades max " + MAX_CONTENT_LENGTH + " tegn (inkl formattering)";
		}

		return null;
	}

	public String sanitizeContent(String content) {
		return sanitize(content).content();
	}

	public SanitizedContent sanitize(String content) {
		Set<String> removed = new LinkedHashSet<>();
		String sanitized = CONTENT_POLICY.sanitize(content, REMOVED_HTML_COLLECTOR, removed);

		return new SanitizedContent(sanitized, removed);
	}

	/**
	 * Returns a danish warning describing what the sanitizer removed, or null if nothing was removed
	 */
	public String getRemovedHtmlWarning(SanitizedContent sanitizedContent) {
		if (!sanitizedContent.hasRemoved()) {
			return null;
		}

		return "Følgende HTML er ikke tilladt og er blevet fjernet: " + String.join(", ", sanitizedContent.removed())
				+ ". Et <style> eller <script> uden afslutning fjerner også alt indhold efter sig, så kontroller at resten af beskeden er intakt.";
	}

	public RenderedEmail render(EmailTemplate emailTemplate, Map<EmailTemplatePlaceholder, String> values) {
		return render(emailTemplate.getSubject(), emailTemplate.getContent(), emailTemplate.getS3AttachmentKey(), values);
	}

	/**
	 * Reserves a test email for the user if they have not sent one within TEST_EMAIL_INTERVAL.
	 * Returns 0 if the test email may be sent, otherwise the number of seconds the user has to wait.
	 */
	public long reserveTestEmail(String user) {
		Instant now = Instant.now();
		Instant cutoff = now.minus(TEST_EMAIL_INTERVAL);

		// clean up expired entries so the map does not keep every user who ever sent a test email
		lastTestEmailSent.values().removeIf(sent -> sent.isBefore(cutoff));

		Instant previous = lastTestEmailSent.putIfAbsent(user, now);
		if (previous == null) {
			return 0;
		}

		return Math.max(1, Duration.between(now, previous.plus(TEST_EMAIL_INTERVAL)).toSeconds());
	}

	/**
	 * Sends the supplied (possibly unsaved) subject and content directly to the recipient (bypassing the mail queue),
	 * with all placeholders replaced by their example values. In development mode the email is logged instead of sent.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public TestEmailResult sendTestEmail(EmailTemplate emailTemplate, String subject, String content, String recipient) {
		EmailTemplateType templateType = emailTemplate.getTemplateType();
		Map<EmailTemplatePlaceholder, String> exampleValues = templateType.getPlaceholders().stream()
				.collect(Collectors.toMap(Function.identity(), EmailTemplatePlaceholder::getExample));
		RenderedEmail email = render(subject.trim(), sanitizeContent(content), emailTemplate.getS3AttachmentKey(), exampleValues);

		if (configuration.isDevelopmentMode()) {
			log.info("Development mode is enabled - not sending test email for {} to {}. Subject: '{}', attachment: {}, content: {}", templateType, recipient, email.subject(), email.attachmentKey(), email.content());
			return TestEmailResult.LOGGED;
		}

		return messageService.sendMailImmediately(recipient, email) ? TestEmailResult.SENT : TestEmailResult.FAILED;
	}

	private RenderedEmail render(String subject, String content, String attachmentKey, Map<EmailTemplatePlaceholder, String> values) {
		for (Map.Entry<EmailTemplatePlaceholder, String> entry : values.entrySet()) {
			String value = entry.getValue() != null ? entry.getValue() : "";
			subject = subject.replace(entry.getKey().getPlaceholder(), value);

			// the content is HTML, so values like a name with '&' or a password with '<' must be escaped to not break (or inject into) the markup
			String escapedValue = HtmlUtils.htmlEscape(value, "UTF-8");
			content = content.replace(entry.getKey().getPlaceholder(), escapedValue);

			// the sanitizer url encodes the braces when a placeholder is used in a link, fx <a href="{link}"> is saved as <a href="%7blink%7d">
			for (String encodedPlaceholder : getUrlEncodedPlaceholders(entry.getKey())) {
				content = content.replace(encodedPlaceholder, escapedValue);
			}
		}

		return new RenderedEmail(subject, content, attachmentKey);
	}

	private static List<String> getUrlEncodedPlaceholders(EmailTemplatePlaceholder placeholder) {
		String placeholderText = placeholder.getPlaceholder();
		String name = placeholderText.substring(1, placeholderText.length() - 1);

		return List.of("%7b" + name + "%7d", "%7B" + name + "%7D");
	}

	private EmailTemplate getOrCreate(EmailTemplateType templateType) {
		EmailTemplate emailTemplate = emailTemplateDao.findByTemplateType(templateType);
		boolean changed = false;

		if (emailTemplate == null) {
			emailTemplate = new EmailTemplate();
			emailTemplate.setTemplateType(templateType);
			changed = true;
		}

		// migrated templates can be missing a subject or content, in which case we fall back to the defaults
		if (!StringUtils.hasText(emailTemplate.getSubject())) {
			emailTemplate.setSubject(templateType.getDefaultSubject());
			changed = true;
		}

		if (!StringUtils.hasText(emailTemplate.getContent())) {
			emailTemplate.setContent(templateType.getDefaultContent());
			changed = true;
		}

		if (changed) {
			emailTemplate.setLastUpdated(LocalDateTime.now());
			emailTemplate = emailTemplateDao.save(emailTemplate);
		}

		return emailTemplate;
	}
}
