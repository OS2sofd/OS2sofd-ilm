package dk.digitalidentity.OS2ilm.model.exception;

public class RoleCatalogIntegrationException extends RuntimeException {
	private static final long serialVersionUID = 3560594447896674710L;

	public RoleCatalogIntegrationException(String message) {
		super(message);
	}

	public RoleCatalogIntegrationException(String message, Throwable cause) {
		super(message, cause);
	}

	public RoleCatalogIntegrationException(Throwable cause) {
		super(cause);
	}
}
