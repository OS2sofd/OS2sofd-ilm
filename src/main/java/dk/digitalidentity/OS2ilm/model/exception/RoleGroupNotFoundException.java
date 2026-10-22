package dk.digitalidentity.OS2ilm.model.exception;

public class RoleGroupNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1960594447396684720L;

	public RoleGroupNotFoundException(Long id) {
		super("RoleGroup with id " + id + " not found");
	}
}
