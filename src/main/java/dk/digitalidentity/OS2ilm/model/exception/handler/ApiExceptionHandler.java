package dk.digitalidentity.OS2ilm.model.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dk.digitalidentity.OS2ilm.model.exception.ActiveDirectoryIntegrationException;
import dk.digitalidentity.OS2ilm.model.exception.NotCommittedException;
import dk.digitalidentity.OS2ilm.model.exception.RoleCatalogIntegrationException;
import dk.digitalidentity.OS2ilm.model.exception.SofdValidationException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotCommittedException.class)
    @ResponseBody
    public static ResponseEntity<?> handleInvalidRequestException(final NotCommittedException ex) {
        return new ResponseEntity<>("The following changes would have been made in the DB:" + ex.getMessage(), HttpStatus.OK);
    }

    @ExceptionHandler(RoleCatalogIntegrationException.class)
    @ResponseBody
    public ResponseEntity<?> handleRoleCatalogIntegrationException(final RoleCatalogIntegrationException ex) {
        log.warn("Role catalog integration failed: {}", ex.getMessage());
        return new ResponseEntity<>("Der opstod en fejl i forbindelse med opdatering af roller: " + ex.getMessage(), HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(ActiveDirectoryIntegrationException.class)
    @ResponseBody
    public ResponseEntity<?> handleActiveDirectoryIntegrationException(final ActiveDirectoryIntegrationException ex) {
        log.warn("Active Directory integration failed: {}", ex.getMessage());
        return new ResponseEntity<>("Der opstod en fejl i forbindelse med opdatering af grupper i Active Directory: " + ex.getMessage(), HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(SofdValidationException.class)
    @ResponseBody
    public ResponseEntity<?> handleSofdValidationException(final SofdValidationException ex) {
        log.warn("SOFD message validation failed: {}", ex.getMessage());
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

}
