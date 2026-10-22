ALTER TABLE employee ADD COLUMN exchange_account_status VARCHAR(255) NOT NULL DEFAULT 'NONE';
ALTER TABLE employee_aud ADD COLUMN exchange_account_status VARCHAR(255);
