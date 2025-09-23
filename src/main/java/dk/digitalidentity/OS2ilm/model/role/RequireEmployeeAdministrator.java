package dk.digitalidentity.OS2ilm.model.role;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ROLE_EMPLOYEE_ADMINISTRATOR') || hasRole('ROLE_SYSTEM_ADMINISTRATOR')")
public @interface RequireEmployeeAdministrator {

}
