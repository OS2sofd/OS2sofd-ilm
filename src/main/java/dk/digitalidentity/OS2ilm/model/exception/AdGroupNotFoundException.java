package dk.digitalidentity.OS2ilm.model.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Service class to handle employee relationship collections and avoid circular dependencies
*/

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AdGroupNotFoundException extends RuntimeException {
	private static final long serialVersionUID = -3781611463175855492L;

	public AdGroupNotFoundException(long id) {
		super("AdGroup with id " + id + " not found");
	}
}
