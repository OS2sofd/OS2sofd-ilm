package dk.digitalidentity.OS2ilm.model.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@SuppressWarnings("serial")
@ResponseStatus(value=HttpStatus.NOT_FOUND, reason = "No such archetype")
public class ArchetypeNotFoundException extends Exception {

	public ArchetypeNotFoundException(String message) {
		super(message);
	}
}
