package dk.digitalidentity.OS2ilm.exception;

public class ADOperationException extends RuntimeException {
	private static final long serialVersionUID = -1313456696067051064L;

	public ADOperationException(String message) {
		super(message);
	}

	public ADOperationException(String message, Throwable cause) {
		super(message, cause);
	}
}
