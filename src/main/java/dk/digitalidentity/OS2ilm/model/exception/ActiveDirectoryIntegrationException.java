package dk.digitalidentity.OS2ilm.model.exception;

public class ActiveDirectoryIntegrationException extends RuntimeException {
	private static final long serialVersionUID = 2007383699472403611L;

	public ActiveDirectoryIntegrationException(String message) {
		super(message);
	}

	public ActiveDirectoryIntegrationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ActiveDirectoryIntegrationException(Throwable cause) {
		super(cause);
	}

}
