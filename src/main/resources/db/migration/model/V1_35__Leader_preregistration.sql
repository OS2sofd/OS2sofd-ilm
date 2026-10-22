-- Add new identifier columns
ALTER TABLE leader ADD COLUMN mit_id_uuid VARCHAR(255);
ALTER TABLE leader ADD COLUMN cpr VARCHAR(10);
ALTER TABLE leader ADD COLUMN internal_uuid VARCHAR(255);

-- Migrate existing uuid to mit_id_uuid
UPDATE leader SET mit_id_uuid = uuid WHERE uuid IS NOT NULL;

-- Create join table for many-to-many
CREATE TABLE leader_business_mapping (
    leader_id BIGINT NOT NULL,
    business_id BIGINT NOT NULL,
    PRIMARY KEY (leader_id, business_id),
    CONSTRAINT fk_lbm_leader FOREIGN KEY (leader_id) REFERENCES leader(id) ON DELETE CASCADE,
    CONSTRAINT fk_lbm_business FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE CASCADE
);

-- Migrate existing relationships
INSERT INTO leader_business_mapping (leader_id, business_id)
SELECT id, business_id FROM leader WHERE business_id IS NOT NULL;

-- Remove old columns
ALTER TABLE leader DROP FOREIGN KEY fk_leader_business;
ALTER TABLE leader DROP COLUMN business_id;
ALTER TABLE leader DROP COLUMN uuid;

-- Add indexes
CREATE INDEX idx_leader_mit_id_uuid ON leader(mit_id_uuid);
CREATE INDEX idx_leader_cpr ON leader(cpr);
CREATE INDEX idx_leader_internal_uuid ON leader(internal_uuid);

-- Audit table for join table
CREATE TABLE leader_business_mapping_aud (
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    leader_id                   BIGINT NOT NULL,
    business_id                 BIGINT NOT NULL,

    FOREIGN KEY fk_leader_business_mapping_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_leader_business_mapping_aud(rev, leader_id, business_id)
);
