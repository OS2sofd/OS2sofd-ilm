package dk.digitalidentity.OS2ilm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.dao.EmailTemplateDao;
import dk.digitalidentity.OS2ilm.model.entity.EmailTemplate;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplateType;
import dk.digitalidentity.OS2ilm.service.EmailTemplateService.RenderedEmail;
import dk.digitalidentity.OS2ilm.service.EmailTemplateService.SanitizedContent;
import dk.digitalidentity.OS2ilm.service.EmailTemplateService.TestEmailResult;

@ExtendWith(MockitoExtension.class)
class EmailTemplateServiceTest {

	@Mock private EmailTemplateDao emailTemplateDao;
	@Mock private MessageService messageService;
	@Mock private IlmConfiguration configuration;

	private EmailTemplateService service;

	@BeforeEach
	void setUp() {
		service = new EmailTemplateService(emailTemplateDao, messageService, configuration);
	}

	private static EmailTemplate template(EmailTemplateType type) {
		EmailTemplate emailTemplate = new EmailTemplate();
		emailTemplate.setTemplateType(type);
		return emailTemplate;
	}

	@Nested
	class SendTestEmail {

		@Test
		void logsInsteadOfSendingInDevelopmentMode() {
			when(configuration.isDevelopmentMode()).thenReturn(true);

			TestEmailResult result = service.sendTestEmail(template(EmailTemplateType.EMPLOYEE_CREATED), "Emne", "Kære {modtager}", "admin@kommune.dk");

			assertThat(result).isEqualTo(TestEmailResult.LOGGED);
			verify(messageService, never()).sendMailImmediately(anyString(), any(RenderedEmail.class));
		}

		@Test
		void sendsWithExampleValuesWhenNotInDevelopmentMode() {
			when(configuration.isDevelopmentMode()).thenReturn(false);
			when(messageService.sendMailImmediately(anyString(), any(RenderedEmail.class))).thenReturn(true);

			TestEmailResult result = service.sendTestEmail(template(EmailTemplateType.EMPLOYEE_EXTENDED), " Hej {modtager} ", "Forlænget til {dato}", "admin@kommune.dk");

			assertThat(result).isEqualTo(TestEmailResult.SENT);
			verify(messageService).sendMailImmediately("admin@kommune.dk", new RenderedEmail("Hej Hans Hansen", "Forlænget til 2026-12-31", null));
		}

		@Test
		void reportsFailureWhenTheMailCouldNotBeSent() {
			when(configuration.isDevelopmentMode()).thenReturn(false);
			when(messageService.sendMailImmediately(anyString(), any(RenderedEmail.class))).thenReturn(false);

			TestEmailResult result = service.sendTestEmail(template(EmailTemplateType.EMPLOYEE_CREATED), "Emne", "Besked", "admin@kommune.dk");

			assertThat(result).isEqualTo(TestEmailResult.FAILED);
		}

		@Test
		void sanitizesContentBeforeSending() {
			when(configuration.isDevelopmentMode()).thenReturn(false);

			service.sendTestEmail(template(EmailTemplateType.EMPLOYEE_CREATED), "Emne", "<p>Hej</p><script>alert(1)</script>", "admin@kommune.dk");

			verify(messageService).sendMailImmediately("admin@kommune.dk", new RenderedEmail("Emne", "<p>Hej</p>", null));
		}
	}

	@Nested
	class Render {

		@Test
		void escapesPlaceholderValuesInContentButNotInSubject() {
			EmailTemplate emailTemplate = template(EmailTemplateType.EMPLOYEE_PASSWORD_RESET);
			emailTemplate.setSubject("Hej {modtager}");
			emailTemplate.setContent("<p>Kære {modtager}, dit kodeord er {kodeord}</p>");

			RenderedEmail email = service.render(emailTemplate, Map.of(
					EmailTemplatePlaceholder.RECIPIENT, "Tom & Jerry <script>",
					EmailTemplatePlaceholder.PASSWORD, "a<b>\"c'"));

			assertThat(email.subject()).isEqualTo("Hej Tom & Jerry <script>");
			assertThat(email.content()).isEqualTo("<p>Kære Tom &amp; Jerry &lt;script&gt;, dit kodeord er a&lt;b&gt;&quot;c&#39;</p>");
		}

		@Test
		void keepsDanishCharactersUnescaped() {
			EmailTemplate emailTemplate = template(EmailTemplateType.EMPLOYEE_CREATED);
			emailTemplate.setSubject("Emne");
			emailTemplate.setContent("{modtager}");

			RenderedEmail email = service.render(emailTemplate, Map.of(EmailTemplatePlaceholder.RECIPIENT, "Søren Ærø Åsen"));

			assertThat(email.content()).isEqualTo("Søren Ærø Åsen");
		}

		@Test
		void replacesPlaceholderInLinkAfterSanitizing() {
			EmailTemplate emailTemplate = template(EmailTemplateType.AWAITING_APPROVAL);
			emailTemplate.setSubject("Emne");
			emailTemplate.setContent(service.sanitizeContent("<p><a href=\"{link}\">Åbn</a> {link}</p>"));

			RenderedEmail email = service.render(emailTemplate, Map.of(EmailTemplatePlaceholder.LINK, "https://os2ilm.kommune.dk/approve?a=1&b=2"));

			assertThat(email.content()).contains("href=\"https://os2ilm.kommune.dk/approve?a=1&amp;b=2\"")
					.contains("</a> https://os2ilm.kommune.dk/approve?a=1&amp;b=2</p>")
					.doesNotContain("link");
		}
	}

	@Nested
	class Sanitize {

		@Test
		void reportsNothingForAllowedHtml() {
			SanitizedContent result = service.sanitize("<p style=\"text-align:center\"><b>Hej</b> <a href=\"https://example.com\">link</a></p>");

			assertThat(result.hasRemoved()).isFalse();
			assertThat(service.getRemovedHtmlWarning(result)).isNull();
		}

		@Test
		void reportsRemovedTagsAndAttributes() {
			SanitizedContent result = service.sanitize("<p onclick=\"alert(1)\">Hej</p><script>alert(2)</script><a href=\"javascript:alert(3)\">x</a>");

			assertThat(result.content()).doesNotContain("script", "onclick", "javascript");
			// an <a> without any allowed attributes left is removed entirely
			assertThat(result.removed()).contains("<script>", "onclick på <p>", "<a>");
			assertThat(service.getRemovedHtmlWarning(result)).contains("<script>");
		}

		@Test
		void reportsAnUnclosedStyleTagThatSwallowsTheRestOfTheContent() {
			SanitizedContent result = service.sanitize("<p>Før</p><style><p>Efter</p>");

			assertThat(result.content()).isEqualTo("<p>Før</p>");
			assertThat(result.removed()).contains("<style>");
		}
	}

	@Nested
	class ReserveTestEmail {

		@Test
		void allowsTheFirstTestEmail() {
			assertThat(service.reserveTestEmail("admin@kommune.dk")).isZero();
		}

		@Test
		void blocksASecondTestEmailWithinTheInterval() {
			service.reserveTestEmail("admin@kommune.dk");

			long secondsToWait = service.reserveTestEmail("admin@kommune.dk");

			assertThat(secondsToWait).isBetween(1L, EmailTemplateService.TEST_EMAIL_INTERVAL.toSeconds());
		}

		@Test
		void limitsEachUserSeparately() {
			service.reserveTestEmail("admin@kommune.dk");

			assertThat(service.reserveTestEmail("other@kommune.dk")).isZero();
		}
	}

	@Nested
	class Validate {

		@Test
		void rejectsColonInSubjectForDigitalPostTemplates() {
			String error = service.validate(template(EmailTemplateType.EMPLOYEE_CREATED), "OS2ILM: Ny adgang", "Besked");

			assertThat(error).contains(":");
		}

		@Test
		void allowsColonInSubjectForEmailOnlyTemplates() {
			String error = service.validate(template(EmailTemplateType.MAIL_INVITATION), "OS2ILM: Invitation", "Besked");

			assertThat(error).isNull();
		}

		@Test
		void rejectsBlankSubject() {
			assertThat(service.validate(template(EmailTemplateType.MAIL_INVITATION), " ", "Besked")).isNotNull();
		}
	}
}
