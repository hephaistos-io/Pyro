import {describe, it, expect} from 'vitest';

/**
 * Tests for the isValidReturnUrl function used in LoginComponent.
 * This function prevents open redirect attacks by validating that return URLs
 * are safe relative paths within the application.
 */

// Extract the validation logic for testing
function isValidReturnUrl(url: string | undefined): boolean {
  if (!url) {
    return false;
  }
  // Must start with single forward slash (relative path)
  // Reject protocol-relative URLs (//), absolute URLs, and javascript: schemes
  return url.startsWith('/') && !url.startsWith('//') && !url.includes(':');
}

describe('isValidReturnUrl', () => {
  describe('valid internal URLs', () => {
    it('accepts simple relative paths', () => {
      expect(isValidReturnUrl('/dashboard')).toBe(true);
      expect(isValidReturnUrl('/settings')).toBe(true);
      expect(isValidReturnUrl('/apps/123')).toBe(true);
    });

    it('accepts paths with query parameters', () => {
      expect(isValidReturnUrl('/dashboard?tab=overview')).toBe(true);
      expect(isValidReturnUrl('/apps?filter=active')).toBe(true);
    });

    it('accepts paths with fragments', () => {
      expect(isValidReturnUrl('/settings#profile')).toBe(true);
    });

    it('accepts nested paths', () => {
      expect(isValidReturnUrl('/apps/123/environments/456')).toBe(true);
    });
  });

  describe('invalid external URLs', () => {
    it('rejects protocol-relative URLs', () => {
      expect(isValidReturnUrl('//evil.com')).toBe(false);
      expect(isValidReturnUrl('//evil.com/path')).toBe(false);
    });

    it('rejects absolute URLs with http', () => {
      expect(isValidReturnUrl('http://evil.com')).toBe(false);
      expect(isValidReturnUrl('http://evil.com/path')).toBe(false);
    });

    it('rejects absolute URLs with https', () => {
      expect(isValidReturnUrl('https://evil.com')).toBe(false);
      expect(isValidReturnUrl('https://evil.com/path')).toBe(false);
    });

    it('rejects javascript: URLs', () => {
      expect(isValidReturnUrl('javascript:alert(1)')).toBe(false);
      expect(isValidReturnUrl('javascript:void(0)')).toBe(false);
    });

    it('rejects data: URLs', () => {
      expect(isValidReturnUrl('data:text/html,<script>alert(1)</script>')).toBe(false);
    });

    it('rejects URLs without leading slash', () => {
      expect(isValidReturnUrl('dashboard')).toBe(false);
      expect(isValidReturnUrl('evil.com')).toBe(false);
    });
  });

  describe('edge cases', () => {
    it('rejects undefined', () => {
      expect(isValidReturnUrl(undefined)).toBe(false);
    });

    it('rejects empty string', () => {
      expect(isValidReturnUrl('')).toBe(false);
    });

    it('rejects null-ish values', () => {
      expect(isValidReturnUrl(null as unknown as string)).toBe(false);
    });

    it('accepts root path', () => {
      expect(isValidReturnUrl('/')).toBe(true);
    });
  });
});
