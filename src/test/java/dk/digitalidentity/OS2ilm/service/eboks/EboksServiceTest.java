package dk.digitalidentity.OS2ilm.service.eboks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.config.settings.modules.EBoks;
import dk.digitalidentity.OS2ilm.exception.EboksException;
import dk.digitalidentity.OS2ilm.exception.RetryableException;
import dk.digitalidentity.OS2ilm.service.AttachmentService;
import dk.digitalidentity.simple_queue.QueueMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;

@ExtendWith(MockitoExtension.class)
class EboksServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private IlmConfiguration configuration;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private EBoks eboksConfig;

    @Mock
    private AttachmentService attachmentService;

    private EboksService service;

    private static final String VALID_CPR = "3002991122";
    // Minimal well-formed XHTML that ITextRenderer can process without external resources
    private static final String MINIMAL_XHTML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
                    + "<head><title>t</title></head>"
                    + "<body><p>test</p></body>"
                    + "</html>";

    @BeforeEach
    void setUp() {
        service = new EboksService(eventPublisher, configuration, templateEngine, attachmentService);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        lenient().when(configuration.isDevelopmentMode()).thenReturn(false);
        lenient().when(configuration.getEboks()).thenReturn(eboksConfig);
        lenient().when(eboksConfig.isEnabled()).thenReturn(true);
    }

    private QueueMessage messageWithBody(String body) {
        return QueueMessage.builder().queue("eboksQueue").priority(1L).body(body).build();
    }

    private QueueMessage validQueueMessage() {
        return messageWithBody("{\"subject\":\"Subject\",\"message\":\"Hello\",\"cpr\":\"" + VALID_CPR + "\"}");
    }

    @Nested
    class HandleMessage {

        @Test
        void returnsConsumed_whenSendStatusIsSend() {
            EboksService spy = spy(service);
            doReturn(EboksService.SendStatus.SEND).when(spy).sendMessage(anyString(), anyString(), anyString(), isNull());

            assertThat(spy.handleMessage(validQueueMessage())).isTrue();
        }

        @Test
        void returnsKeep_whenSendStatusIsDisabled() {
            EboksService spy = spy(service);
            doReturn(EboksService.SendStatus.DISABLED).when(spy).sendMessage(anyString(), anyString(), anyString(), isNull());

            assertThat(spy.handleMessage(validQueueMessage())).isFalse();
        }

        @Test
        void throwsEboksException_whenSendStatusIsInvalidRecipient() {
            EboksService spy = spy(service);
            doReturn(EboksService.SendStatus.INVALID_RECIPIENT)
                    .when(spy)
                    .sendMessage(anyString(), anyString(), anyString(), isNull());

            assertThatThrownBy(() -> spy.handleMessage(validQueueMessage()))
                    .isInstanceOf(EboksException.class)
                    .hasMessageContaining("invalid CPR");
        }

        @Test
        void throwsEboksException_whenSendStatusIsRecipientNotSubscribed() {
            EboksService spy = spy(service);
            doReturn(EboksService.SendStatus.RECIPIENT_NOT_SUBSCRIBED)
                    .when(spy)
                    .sendMessage(anyString(), anyString(), anyString(), isNull());

            assertThatThrownBy(() -> spy.handleMessage(validQueueMessage()))
                    .isInstanceOf(EboksException.class)
                    .hasMessageContaining("not subscribed");
        }

        @Test
        void throwsRetryableException_whenSendStatusIsTechnicalError() {
            EboksService spy = spy(service);
            doReturn(EboksService.SendStatus.TECHNICAL_ERROR)
                    .when(spy)
                    .sendMessage(anyString(), anyString(), anyString(), isNull());

            assertThatThrownBy(() -> spy.handleMessage(validQueueMessage()))
                    .isInstanceOf(RetryableException.class);
        }

        @Test
        void throwsEboksException_onInvalidJson() {
            assertThatThrownBy(() -> service.handleMessage(messageWithBody("not-valid-json")))
                    .isInstanceOf(EboksException.class)
                    .hasMessageContaining("could not parse JSON body");
        }

        @Test
        void wrapsUnexpectedExceptionInRetryableException() {
            EboksService spy = spy(service);
            doThrow(new RuntimeException("unexpected"))
                    .when(spy)
                    .sendMessage(anyString(), anyString(), anyString(), isNull());

            assertThatThrownBy(() -> spy.handleMessage(validQueueMessage()))
                    .isInstanceOf(RetryableException.class)
                    .hasMessageContaining("Fejl ved behandling");
        }
    }

    @Nested
    class SendMessage {

        @Test
        void returnsDisabled_inDevelopmentMode() {
            when(configuration.isDevelopmentMode()).thenReturn(true);

            assertThat(service.sendMessage("Hello", "Subject", VALID_CPR, null))
                    .isEqualTo(EboksService.SendStatus.DISABLED);
        }

        @Test
        void returnsDisabled_whenEboksNotEnabled() {
            when(eboksConfig.isEnabled()).thenReturn(false);

            assertThat(service.sendMessage("Hello", "Subject", VALID_CPR, null))
                    .isEqualTo(EboksService.SendStatus.DISABLED);
        }

        @Test
        void returnsInvalidRecipient_forNullCpr() {
            assertThat(service.sendMessage("Hello", "Subject", null, null))
                    .isEqualTo(EboksService.SendStatus.INVALID_RECIPIENT);
        }

        @Test
        void returnsInvalidRecipient_forShortCpr() {
            assertThat(service.sendMessage("Hello", "Subject", "123456789", null))
                    .isEqualTo(EboksService.SendStatus.INVALID_RECIPIENT);
        }

        @Test
        void returnsInvalidRecipient_forNonNumericCpr() {
            assertThat(service.sendMessage("Hello", "Subject", "010101ABCD", null))
                    .isEqualTo(EboksService.SendStatus.INVALID_RECIPIENT);
        }

        @Test
        void returnsTechnicalError_whenSenderNameMissing() {
            when(eboksConfig.getSenderName()).thenReturn(null);

            assertThat(service.sendMessage("Hello", "Subject", VALID_CPR, null))
                    .isEqualTo(EboksService.SendStatus.TECHNICAL_ERROR);
        }

        @Test
        void returnsSend_on200Response() {
            stubEboksConfigForHttpTests();
            when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(""));

            assertThat(service.sendMessage("Hello", "Subject", VALID_CPR, null))
                    .isEqualTo(EboksService.SendStatus.SEND);
        }

        @Test
        void returnsRecipientNotSubscribed_on409InResponseBody() {
            stubEboksConfigForHttpTests();
            when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.status(409).body(""));

            assertThat(service.sendMessage("Hello", "Subject", VALID_CPR, null))
                    .isEqualTo(EboksService.SendStatus.RECIPIENT_NOT_SUBSCRIBED);
        }

        @Test
        void returnsRecipientNotSubscribed_on409Exception() {
            stubEboksConfigForHttpTests();
            when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                    .thenThrow(HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", null, null, null));

            assertThat(service.sendMessage("Hello", "Subject", VALID_CPR, null))
                    .isEqualTo(EboksService.SendStatus.RECIPIENT_NOT_SUBSCRIBED);
        }

        @Test
        void returnsTechnicalError_onOtherHttpException() {
            stubEboksConfigForHttpTests();
            when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                    .thenThrow(HttpClientErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Error", null, null, null));

            assertThat(service.sendMessage("Hello", "Subject", VALID_CPR, null))
                    .isEqualTo(EboksService.SendStatus.TECHNICAL_ERROR);
        }

        @Test
        @SuppressWarnings("unchecked")
        void includesAttachmentFromS3AsBase64() {
            stubEboksConfigForHttpTests();
            when(attachmentService.load("kommune/emailtemplate/vejledning.pdf"))
                    .thenReturn(Optional.of(new AttachmentService.FileAttachment("vejledning.pdf", "pdf-indhold".getBytes())));
            when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(""));

            service.sendMessage("Hello", "Subject", VALID_CPR, "kommune/emailtemplate/vejledning.pdf");

            ArgumentCaptor<HttpEntity<EboksMessage>> request = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).postForEntity(anyString(), request.capture(), eq(String.class));
            assertThat(request.getValue().getBody().getAttachments())
                    .containsExactly(new EboksMessage.EboksAttachment("vejledning.pdf", Base64.getEncoder().encodeToString("pdf-indhold".getBytes())));
        }

        @Test
        void throwsAndDoesNotSend_whenAttachmentCannotBeReadFromS3() {
            when(eboksConfig.getSenderName()).thenReturn("Test Kommune");
            when(eboksConfig.getCvr()).thenReturn("12345678");
            when(templateEngine.process(anyString(), any())).thenReturn(MINIMAL_XHTML);
            when(attachmentService.load("missing.pdf")).thenThrow(new RuntimeException("S3 nede"));

            assertThatThrownBy(() -> service.sendMessage("Hello", "Subject", VALID_CPR, "missing.pdf"))
                    .hasMessage("S3 nede");
            verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
        }

        private void stubEboksConfigForHttpTests() {
            when(eboksConfig.getSenderName()).thenReturn("Test Kommune");
            when(eboksConfig.getCvr()).thenReturn("12345678");
            when(eboksConfig.getUrl()).thenReturn("http://test.example");
            when(templateEngine.process(anyString(), any())).thenReturn(MINIMAL_XHTML);
        }
    }

    @Nested
    class HandleFailedMessage {

        @Test
        void reEnqueues_onRetryableException_belowMaxAttempts() {
            QueueMessage msg = QueueMessage.builder()
                    .queue("eboksQueue")
                    .priority(1L)
                    .body("body")
                    .headers(Map.of("x-attempt", "0"))
                    .build();

            boolean result = service.handleFailedMessage(msg, new RetryableException("transient"));

            assertThat(result).isTrue();
            verify(eventPublisher).publishEvent(any(QueueMessage.class));
        }

        @Test
        void deadLetters_onNonRetryableException() {
            QueueMessage msg = QueueMessage.builder()
                    .queue("eboksQueue")
                    .priority(1L)
                    .body("body")
                    .headers(Map.of("x-attempt", "0"))
                    .build();

            boolean result = service.handleFailedMessage(msg, new EboksException("permanent failure"));

            assertThat(result).isFalse();
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    class AddMessageToQueue {

        @Test
        void returnsFalse_forNullMessage() {
            assertThat(service.addMessageToQueue(null)).isFalse();
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void returnsFalse_whenCprIsNull() {
            assertThat(service.addMessageToQueue(new EboksService.EboksMessageRequest("subj", "msg", null, null)))
                    .isFalse();
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void returnsFalse_whenCprIsWrongLength() {
            assertThat(service.addMessageToQueue(new EboksService.EboksMessageRequest("subj", "msg", "123456789", null)))
                    .isFalse();
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void returnsTrue_andPublishesEvent_forValidMessage() {
            boolean result = service.addMessageToQueue(
                    new EboksService.EboksMessageRequest("Subject", "Body", VALID_CPR, null));

            assertThat(result).isTrue();
            verify(eventPublisher).publishEvent(any(QueueMessage.class));
        }
    }
}
