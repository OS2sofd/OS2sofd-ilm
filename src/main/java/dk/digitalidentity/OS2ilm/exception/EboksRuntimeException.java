package dk.digitalidentity.OS2ilm.exception;

public class EboksRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 2763782802295084959L;

	public EboksRuntimeException(String message) {
		super(message);
	}

	public EboksRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}
}
