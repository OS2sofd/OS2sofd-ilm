package dk.digitalidentity.OS2ilm.model.role;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ROLE_LEADER') ||hasRole('ROLE_SYSTEM_READONLY') || hasRole('ROLE_SYSTEM_ADMINISTRATOR')")
@RequireRoleAnnotation
public @interface RequireLeaderOrHigher {
}
