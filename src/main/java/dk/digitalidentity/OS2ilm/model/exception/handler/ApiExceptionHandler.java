package dk.digitalidentity.OS2ilm.model.exception.handler;

import dk.digitalidentity.OS2ilm.model.exception.NotCommittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(NotCommittedException.class)
    @ResponseBody
    public static ResponseEntity<?> handleInvalidRequestException(final NotCommittedException ex) {
        return new ResponseEntity<>("The following changes would have been made in the DB:" + ex.getMessage(), HttpStatus.OK);
    }

}
