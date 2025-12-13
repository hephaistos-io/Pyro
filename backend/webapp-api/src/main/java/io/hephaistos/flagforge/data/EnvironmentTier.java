package io.hephaistos.flagforge.data;

/**
 * Represents the tier of an environment. FREE environments are created by default with each
 * application and cannot be deleted. PAID environments are additional environments that can be
 * created and deleted.
 */
public enum EnvironmentTier {
    FREE,
    PAID
}
