package dk.digitalidentity.OS2ilm.model.role;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@RequireRoleAnnotation
public @interface NoRoleRequired {
}
