-- Revert API key storage from hashed to plaintext
-- Existing keys stored as hashes are now invalid and must be deleted
-- New keys will be stored in plaintext and can be displayed to users

DELETE
FROM api_key;

COMMENT ON COLUMN api_key.key_hash IS 'Plaintext API key (64 hex characters). Stored in plaintext for user visibility.';
