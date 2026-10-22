ALTER TABLE business_employment_settings ADD COLUMN name_strategy VARCHAR(255) NOT NULL DEFAULT 'PREFIXED';
ALTER TABLE business_employment_settings_aud ADD COLUMN name_strategy VARCHAR(255);

ALTER TABLE business_employment_settings ADD COLUMN account_name_template VARCHAR(255);
ALTER TABLE business_employment_settings_aud ADD COLUMN account_name_template VARCHAR(255);

UPDATE business_employment_settings SET name_strategy = 'SOFD' WHERE sofd_generated_name = TRUE;
UPDATE business_employment_settings SET name_strategy = 'PREFIXED' WHERE sofd_generated_name = FALSE;
