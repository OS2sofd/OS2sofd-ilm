-- Add unique constraints on leader identifier columns (allows multiple NULLs)
ALTER TABLE leader ADD CONSTRAINT uk_leader_mit_id_uuid UNIQUE (mit_id_uuid);
ALTER TABLE leader ADD CONSTRAINT uk_leader_cpr UNIQUE (cpr);
ALTER TABLE leader ADD CONSTRAINT uk_leader_internal_uuid UNIQUE (internal_uuid);
