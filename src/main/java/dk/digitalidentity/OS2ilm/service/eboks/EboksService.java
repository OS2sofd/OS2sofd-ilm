package dk.digitalidentity.OS2ilm.service.eboks;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.exception.EboksException;
import dk.digitalidentity.OS2ilm.exception.RetryableException;
import dk.digitalidentity.OS2ilm.queue.QueueConstants;
import dk.digitalidentity.OS2ilm.queue.RetryableMessageHandler;
import dk.digitalidentity.OS2ilm.service.AttachmentService;
import dk.digitalidentity.simple_queue.QueueMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Slf4j
@Service
public class EboksService extends RetryableMessageHandler {
	private static final String QUEUE_IDENTIFIER = QueueConstants.EBOKS_QUEUE;
	private static final int MAX_ATTEMPTS = Integer.MAX_VALUE;

    public enum SendStatus { SEND, TECHNICAL_ERROR, INVALID_RECIPIENT, RECIPIENT_NOT_SUBSCRIBED, DISABLED }

    private final IlmConfiguration configuration;
    private final TemplateEngine templateEngine;
    private final AttachmentService attachmentService;

    @Autowired
    private RestTemplate restTemplate;

    public EboksService(ApplicationEventPublisher eventPublisher,
            IlmConfiguration configuration, TemplateEngine templateEngine, AttachmentService attachmentService) {
        super(QUEUE_IDENTIFIER, MAX_ATTEMPTS, eventPublisher);
        this.configuration = configuration;
        this.templateEngine = templateEngine;
        this.attachmentService = attachmentService;
    }

    // attachmentKey is the S3 key of an attachment (or null). The file is fetched when the message is sent, so it is not stored on the queue
    public record EboksMessageRequest(String subject, String message, String cpr, String attachmentKey) { }

    @Override
    public boolean handleMessage(QueueMessage message) {
		try {
			EboksMessageRequest msg = stringToObject(message.getBody());

			SendStatus sendStatus = sendMessage(msg.message, msg.subject, msg.cpr, msg.attachmentKey);
			switch (sendStatus) {
				case INVALID_RECIPIENT:
					throw new EboksException("E-boks message failed - invalid CPR");
				case RECIPIENT_NOT_SUBSCRIBED:
					throw new EboksException("E-boks message failed - recipient not subscribed to e-boks");
				case TECHNICAL_ERROR:
					throw new RetryableException("Kunne ikke sende eboks besked: " + sendStatus);
				case DISABLED:
					return KEEP;
				case SEND:
					return CONSUMED;
			}
		}
		catch (JacksonException ex) {
			throw new EboksException("E-boks message failed - could not parse JSON body", ex);
		}
		catch (RetryableException | EboksException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new RetryableException("Fejl ved behandling af eboks besked", ex);
		}

		return KEEP;
	}

    /**
     * Throws if the attachment could not be read from S3, so the queue retries the message later
     */
    public SendStatus sendMessage(String message, String subject, String cpr, String attachmentKey) {
        if (configuration.isDevelopmentMode()) {
			log.warn("Development mode is enabled - not sending e-boks message");
			return SendStatus.DISABLED;
		}

		if (!configuration.getEboks().isEnabled()) {
            log.warn("e-boks server is not configured - not sending digital post!");
            return SendStatus.DISABLED;
        }

        if (!validCpr(cpr)) {
            log.warn("Not a valid CPR (length={}) - skipping eBoks", cpr != null ? cpr.length() : "null");
            return SendStatus.INVALID_RECIPIENT;
        }

        if (!StringUtils.hasLength(configuration.getEboks().getSenderName())) {
            log.error("Bad configuration: missing senderName");
            return SendStatus.TECHNICAL_ERROR;
        }

        if (subject.contains(":")) {
            log.warn("eBoks subject contains ':' which is not allowed by Digital Post - stripping colons from subject: '{}'", subject);
            subject = subject.replace(":", "");
        }

        log.info("Sending e-boks message '{}' to {}", subject, maskCpr(cpr));

        try {
            EboksMessage msg = new EboksMessage();
            msg.setCvr(configuration.getEboks().getCvr());
            msg.setCpr(cpr);
            msg.setSubject(subject);
            msg.setContent(Base64.getEncoder().encodeToString(generatePDF(subject, message)));
            msg.setMunicipalityName(configuration.getEboks().getSenderName());
            attachmentService.load(attachmentKey).ifPresent(attachment -> msg.getAttachments().add(
                    new EboksMessage.EboksAttachment(attachment.filename(), Base64.getEncoder().encodeToString(attachment.content()))));

            HttpHeaders header = new HttpHeaders();
            header.add("Content-Type", "application/json");
            HttpEntity<EboksMessage> request = new HttpEntity<>(msg, header);
            ResponseEntity<String> response = restTemplate.postForEntity(configuration.getEboks().getUrl(), request, String.class);

			log.info("eBoks HTTP response: status={}, body={}", response.getStatusCode().value(), response.getBody());
			if (response.getStatusCode().value() == 200) {
				return SendStatus.SEND;
			}

			if (response.getStatusCode().value() == 409) {
				log.warn("eBoks recipient not subscribed: {}", maskCpr(cpr));
				return SendStatus.RECIPIENT_NOT_SUBSCRIBED;
			}

			log.error("eBoks send failed for {}: HTTP {}", maskCpr(cpr), response.getStatusCode().value());
			return SendStatus.TECHNICAL_ERROR;
		}
        catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 409) {
                log.warn("eBoks recipient not subscribed: {}", maskCpr(cpr));
                return SendStatus.RECIPIENT_NOT_SUBSCRIBED;
            }
            else {
                log.error("eBoks send failed for {}", maskCpr(cpr), ex);
                return SendStatus.TECHNICAL_ERROR;
            }
        }
    }

    private static String maskCpr(String cpr) {
        if (cpr == null) {
            return "INVALID";
        }
        String stripped = cpr.replace("-", "").replace(" ", "");
        if (stripped.length() != 10) {
            return "INVALID";
        }
        return stripped.substring(0, 6) + "-XXXX";
    }


    private byte[] generatePDF(String subject, String message) {
        // fix escape characters (with space after, to ensure it does not affect actually escape-stuff)
        message = message.replace("& ", "&amp; ");

		// Fix self-closing tags for XHTML compliance
		message = message.replace("<br>", "<br/>");
		message = message.replace("<BR>", "<br/>");
		message = message.replace("<hr>", "<hr/>");
		message = message.replace("<HR>", "<hr/>");

        Context ctx = new Context();
        ctx.setVariable("subject", subject);
        ctx.setVariable("message", message);

		String htmlContent = templateEngine.process("pdf/template", ctx);

        // Create PDF document and return as byte[]
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);

			return outputStream.toByteArray();
        }
        catch (Exception ex) {
            log.error("Failed to generate pdf", ex);
            return null;
        }
    }

    // copied from NemLoginService
    private boolean validCpr(String cpr) {
        if (cpr == null || cpr.length() != 10) {
            return false;
        }

        for (char c : cpr.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }

        int days = Integer.parseInt(cpr.substring(0, 2));
        int month = Integer.parseInt(cpr.substring(2, 4));

        if (days < 1 || days > 31) {
            return false;
        }

        if (month < 1 || month > 12) {
            return false;
        }

        return true;
    }

    public boolean addMessageToQueue(EboksMessageRequest message) {
        if (message == null || message.cpr == null || message.cpr.length() != 10) {
            log.warn("There was no message or CPR number! aborting...");
            return false;
        }

        try {
            eventPublisher.publishEvent(QueueMessage.builder()
                    .queue("eboksQueue")
                    .priority(1L)
                    .body(objectToJson(message))
                    .build());
    	}
    	catch (Exception ex) {
    		log.error("Failed to queue message", ex);
    		return false;
    	}
        
        return true;
    }

    private String objectToJson(Object object) throws JacksonException {
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    private EboksMessageRequest stringToObject(String body) throws JacksonException {
        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(body, EboksMessageRequest.class);
    }

	@Override
	protected long backoffMinutes(int attempt) {
		return Math.min(360, (long) Math.pow(2, attempt)); // 1, 2, 4, 8, 16, 32, 64, 128, 256, 360, 360, 360...
	}
}
