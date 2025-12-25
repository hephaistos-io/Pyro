package io.hephaistos.flagforge.common.cache;

/**
 * Type of cache invalidation event.
 *
 * <ul>
 *   <li>SCHEMA_CHANGE: Template schema was updated - invalidates all caches for that template type
 *   <li>OVERRIDE_CHANGE: Template override was created/updated/deleted - invalidates specific cache entries
 *   <li>USER_CHANGE: User-specific override was changed - invalidates specific user cache entry
 * </ul>
 */
public enum CacheInvalidationType {
    SCHEMA_CHANGE,
    OVERRIDE_CHANGE,
    USER_CHANGE
}
