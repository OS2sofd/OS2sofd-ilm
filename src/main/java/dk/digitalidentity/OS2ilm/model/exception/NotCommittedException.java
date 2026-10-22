package dk.digitalidentity.OS2ilm.model.exception;

public class NotCommittedException extends RuntimeException {
    private static final long serialVersionUID = 1679357656992434010L;

	public NotCommittedException(String message) {
        super(message);
    }
}
