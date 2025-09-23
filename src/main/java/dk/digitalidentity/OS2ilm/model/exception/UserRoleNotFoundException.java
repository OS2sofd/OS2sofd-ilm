package dk.digitalidentity.OS2ilm.model.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Service class to handle employee relationship collections and avoid circular dependencies
*/

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UserRoleNotFoundException extends RuntimeException {
	public UserRoleNotFoundException(long id) {
		super("UserRole with id " + id + " not found");
	}
}
