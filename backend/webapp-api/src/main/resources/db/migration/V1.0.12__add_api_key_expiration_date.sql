-- Add expiration_date column to api_key table
-- This allows API keys to have an expiration date, with a default of year 2100

ALTER TABLE api_key
    ADD COLUMN expiration_date TIMESTAMP WITH TIME ZONE;

-- Set default expiration date for existing keys to January 1, 2100
UPDATE api_key
SET expiration_date = '2100-01-01 00:00:00+00'
WHERE expiration_date IS NULL;

-- Make the column NOT NULL after setting default values
ALTER TABLE api_key
    ALTER COLUMN expiration_date SET NOT NULL;

-- Set default for new records to January 1, 2100
ALTER TABLE api_key
    ALTER COLUMN expiration_date SET DEFAULT '2100-01-01 00:00:00+00';