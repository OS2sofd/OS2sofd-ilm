ALTER TABLE business_employment_settings ADD COLUMN enable_ordering BOOLEAN DEFAULT FALSE;
ALTER TABLE business_employment_settings_aud ADD COLUMN enable_ordering BOOLEAN NULL;
