package dk.digitalidentity.OS2ilm.exception;

public class RetryableException extends RuntimeException {
    public RetryableException(String message, Throwable cause) {
        super(message, cause);
    }

	public RetryableException(String message) {
		super(message);
	}
}
