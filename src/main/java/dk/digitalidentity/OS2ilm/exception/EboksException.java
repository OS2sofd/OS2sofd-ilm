package dk.digitalidentity.OS2ilm.exception;

public class EboksException extends RuntimeException {
	public EboksException(String message) {
		super(message);
	}

	public EboksException(String message, Throwable cause) {
		super(message, cause);
	}
}
