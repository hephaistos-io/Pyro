package io.hephaistos.flagforge.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the current user to have at least the READ_ONLY role. Due to role hierarchy, DEV and
 * ADMIN users are also authorized. This is effectively the same as requiring authentication.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('READ_ONLY')")
public @interface RequireReadOnly {
}
