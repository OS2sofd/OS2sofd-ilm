package dk.digitalidentity.OS2ilm.service.eboks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.simple_queue.QueueMessage;
import dk.digitalidentity.simple_queue.SimpleMessageHandler;
import lombok.RequiredArgsConstructor;
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

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class EboksService implements SimpleMessageHandler {
    public enum SendStatus { SEND, TECHNICAL_ERROR, INVALID_RECIPIENT, RECIPIENT_NOT_SUBSCRIBED, DISABLED }

    private final IlmConfiguration configuration;
    private final TemplateEngine templateEngine;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public boolean handles(QueueMessage message) {
        return "eboksQueue".equals(message.getQueue());
    }

    public record EboksMessageRequest(String subject, String message, String cpr) { }

    @Override
    public boolean handleMessage(QueueMessage message) {
        try {
            EboksMessageRequest msg = stringToObject(message.getBody());

            SendStatus sendStatus = sendMessage(msg.message, msg.subject, msg.cpr);
            switch (sendStatus) {
	            case INVALID_RECIPIENT, RECIPIENT_NOT_SUBSCRIBED, TECHNICAL_ERROR:
	            	throw new RuntimeException("Kunne ikke sende eboks besked: " + sendStatus.toString());
	            case DISABLED:
	            	return false;
	            case SEND:
	            	return true;
            }
        }
        catch (JsonProcessingException ex) {
            throw new RuntimeException("Kan ikke sende eboks besked - json processing", ex);
        }

        return false;
    }

    @Override
    public boolean handleFailedMessage(QueueMessage message, Exception exception) {
        return false;
    }

    public SendStatus sendMessage(String message, String subject, String cpr) {
        if (!configuration.getEboks().isEnabled()) {
            log.warn("e-boks server is not configured - not sending digital post!");
            return SendStatus.DISABLED;
        }

        if (!validCpr(cpr)) {
            log.debug("Not a valid cpr: " + cpr);
            return SendStatus.INVALID_RECIPIENT;
        }

        if (!StringUtils.hasLength(configuration.getEboks().getSenderName())) {
            log.error("Bad configuration: missing senderName");
            return SendStatus.TECHNICAL_ERROR;
        }

        log.debug("Sending e-boks message '" + subject + "' to " + cpr);

        try {
            EboksMessage msg = new EboksMessage();
            msg.setCvr(configuration.getEboks().getCvr());
            msg.setCpr(cpr);
            msg.setSubject(subject);
            msg.setContent(Base64.getEncoder().encodeToString(generatePDF(subject, message)));
            msg.setMunicipalityName(configuration.getEboks().getSenderName());

            HttpHeaders header = new HttpHeaders();
            header.add("Content-Type", "application/json");
            HttpEntity<EboksMessage> request = new HttpEntity<>(msg, header);
            ResponseEntity<String> response = restTemplate.postForEntity(configuration.getEboks().getUrl(), request, String.class);

            if (response.getStatusCode().value() != 200) {
                if (response.getStatusCode().value() == 409) {
                    log.warn("Failed to send e-boks message to: " + cpr + " - person not subscribed");
                    return SendStatus.RECIPIENT_NOT_SUBSCRIBED;
                }
                else {
                    log.error("Failed to send e-boks message to: " + cpr + ". HTTP: " + response.getStatusCode().value());
                    return SendStatus.TECHNICAL_ERROR;
                }
            }
        }
        catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 409) {
                log.warn("e-boks not subscribed: " + cpr);
                return SendStatus.RECIPIENT_NOT_SUBSCRIBED;
            }
            else {
                log.error("Failed to send e-boks message to: " + cpr, ex);
                return SendStatus.TECHNICAL_ERROR;
            }
        }

        return SendStatus.SEND;
    }


    private byte[] generatePDF(String subject, String message) {
        // fix escape characters (with space after, to ensure it does not affect actually escape-stuff)
        message = message.replace("& ", "&amp; ");

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

    private String objectToJson(Object object) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    private EboksMessageRequest stringToObject(String body) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(body, EboksMessageRequest.class);
    }
}
