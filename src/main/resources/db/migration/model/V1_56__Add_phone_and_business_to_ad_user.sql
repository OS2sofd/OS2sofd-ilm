ALTER TABLE ad_user ADD COLUMN phone_number VARCHAR(20) DEFAULT NULL;
ALTER TABLE ad_user ADD COLUMN business_id BIGINT NULL;
ALTER TABLE ad_user ADD CONSTRAINT fk_user_business FOREIGN KEY (business_id) REFERENCES business (id) ON DELETE SET NULL;

ALTER TABLE ad_user_aud ADD COLUMN phone_number VARCHAR(20);
ALTER TABLE ad_user_aud ADD COLUMN business_id BIGINT;
