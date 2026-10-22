-- Add new identifier columns to audit table
ALTER TABLE leader_aud ADD COLUMN mit_id_uuid VARCHAR(255);
ALTER TABLE leader_aud ADD COLUMN cpr VARCHAR(10);
ALTER TABLE leader_aud ADD COLUMN internal_uuid VARCHAR(255);
