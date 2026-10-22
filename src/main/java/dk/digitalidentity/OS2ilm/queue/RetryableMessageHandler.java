package dk.digitalidentity.OS2ilm.queue;

import dk.digitalidentity.OS2ilm.exception.RetryableException;
import dk.digitalidentity.simple_queue.QueueMessage;
import dk.digitalidentity.simple_queue.SimpleMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class RetryableMessageHandler implements SimpleMessageHandler {
    private static final String ATTEMPT_HEADER = "x-attempt";

	// Return aliases for handleMessage
	protected static final boolean CONSUMED = true;
	protected static final boolean KEEP = false;

	// Return aliases for handleFailedMessage / doHandleFailedMessage
	protected static final boolean FAIL_HANDLED = true;
	protected static final boolean FAIL_DEAD_LETTER = false;

	protected final String queueIdentifier;
	protected final int maxAttempts;
    protected final ApplicationEventPublisher eventPublisher;

    protected RetryableMessageHandler(String queueIdentifier, int maxAttempts, ApplicationEventPublisher eventPublisher) {
        this.queueIdentifier = queueIdentifier;
		this.maxAttempts = maxAttempts;
        this.eventPublisher = eventPublisher;
    }

	@Override
	public boolean handles(QueueMessage message) {
		boolean canHandle = queueIdentifier.equals(message.getQueue());
		log.debug("Queue message handler check: queue={}, can_handle={}", message.getQueue(), canHandle);
		return canHandle;
	}

    protected int currentAttempt(QueueMessage message) {
        if (message.getHeaders() == null) {
			return 0;
		}
        String val = message.getHeaders().get(ATTEMPT_HEADER);
        if (val == null) {
            return 0;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            log.warn("Malformed {} header '{}', treating as attempt 0", ATTEMPT_HEADER, val);
            return 0;
        }
    }

    protected boolean isLastAttempt(QueueMessage message) {
        return currentAttempt(message) >= maxAttempts;
    }

    @Override
    public boolean handleFailedMessage(QueueMessage message, Exception ex) {
        int attempt = currentAttempt(message);
        if (!isRetryable(ex) || attempt >= maxAttempts) {
            log.error("Message failed permanently on attempt {}/{}", attempt + 1, maxAttempts + 1, ex);
            return doHandleFailedMessage(message, ex);
        }
        long delayMinutes = backoffMinutes(attempt);
        log.warn("Retryable failure on attempt {}/{}, re-enqueuing with {}m delay", attempt + 1, maxAttempts + 1, delayMinutes);
        Map<String, String> headers = new HashMap<>(message.getHeaders() != null ? message.getHeaders() : Map.of());
        headers.put(ATTEMPT_HEADER, String.valueOf(attempt + 1));
        eventPublisher.publishEvent(QueueMessage.builder()
                .queue(message.getQueue())
                .body(message.getBody())
                .priority(message.getPriority())
                .headers(headers)
                .dequeueTime(Instant.now().plus(Duration.ofMinutes(delayMinutes)))
                .build());
        return FAIL_HANDLED;
    }

    protected boolean doHandleFailedMessage(QueueMessage message, Exception ex) {
        return FAIL_DEAD_LETTER;
    }

    private boolean isRetryable(Exception ex) {
        return ExceptionUtils.getThrowableList(ex).stream()
                .anyMatch(RetryableException.class::isInstance);
    }

    protected long backoffMinutes(int attempt) {
        return Math.min(60, (long) Math.pow(2, attempt)); // 1, 2, 4, 8, 16, 32, 60, 60, 60...
    }
}
