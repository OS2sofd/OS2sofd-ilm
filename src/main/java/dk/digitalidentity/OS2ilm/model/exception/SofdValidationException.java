package dk.digitalidentity.OS2ilm.model.exception;

public class SofdValidationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public SofdValidationException(String message) {
		super(message);
	}

	public SofdValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}
