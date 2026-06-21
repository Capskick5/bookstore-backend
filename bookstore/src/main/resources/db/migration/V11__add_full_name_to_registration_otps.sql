ALTER TABLE registration_otps
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(255);

UPDATE registration_otps
SET full_name = 'BookVerse User'
WHERE full_name IS NULL;
