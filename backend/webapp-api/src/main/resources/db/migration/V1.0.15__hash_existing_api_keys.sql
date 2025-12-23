-- Security fix: API keys are now stored as SHA-256 hashes instead of plaintext
--
-- IMPORTANT: This migration removes all existing API keys because:
-- 1. Existing keys were stored in plaintext
-- 2. We cannot hash and keep them because users would need to regenerate anyway
--    (they don't know the new hashed value matches their key)
-- 3. After this migration, users must regenerate their API keys
--
-- The key_hash column now stores actual SHA-256 hashes (64 hex characters)
-- instead of the plaintext key value.

-- Delete all existing API keys (they were stored in plaintext and are now invalid)
DELETE
FROM api_key;

-- Add comment to clarify the column now stores hashes
COMMENT ON COLUMN api_key.key_hash IS 'SHA-256 hash of the API key (64 hex characters). The plaintext key is only shown once at creation time.';
