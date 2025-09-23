package dk.digitalidentity.OS2ilm.exception;

public class EboksRuntimeException extends RuntimeException {
	public EboksRuntimeException(String message) {
		super(message);
	}

	public EboksRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}
}
