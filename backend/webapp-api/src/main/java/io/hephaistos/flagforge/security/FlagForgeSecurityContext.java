package io.hephaistos.flagforge.security;

/**
 * This class has been moved to backend-common for reuse across modules.
 *
 * @deprecated Use {@link io.hephaistos.flagforge.common.security.FlagForgeSecurityContext} instead.
 */
@Deprecated(forRemoval = true)
public class FlagForgeSecurityContext
        extends io.hephaistos.flagforge.common.security.FlagForgeSecurityContext {
    // Backward compatibility - extends the common implementation
}
