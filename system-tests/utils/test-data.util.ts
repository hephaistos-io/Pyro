/**
 * Common test data utilities
 */

export const DEFAULT_PASSWORD = 'SecurePassword123!@#';

/**
 * Generates a unique email address for testing
 */
export function uniqueEmail(prefix: string = 'test'): string {
    return `${prefix}-${Date.now()}-${crypto.randomUUID().slice(0, 8)}@example.com`;
}

/**
 * Generates a unique name with timestamp
 */
export function uniqueName(prefix: string): string {
    return `${prefix}-${Date.now()}`;
}
