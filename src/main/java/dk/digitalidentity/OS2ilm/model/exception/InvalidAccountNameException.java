package dk.digitalidentity.OS2ilm.model.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidAccountNameException extends RuntimeException {
	@Serial
	private static final long serialVersionUID = -3781611463175855492L;

	public InvalidAccountNameException(String name) {
		super("AD-Username: " + name + " is invalid");
	}
}
