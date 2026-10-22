ALTER TABLE business
ADD COLUMN default_org_unit_uuid VARCHAR(36) NULL;

ALTER TABLE business
ADD CONSTRAINT fk_business_default_orgunit
FOREIGN KEY (default_org_unit_uuid)
REFERENCES orgunit(uuid)
ON DELETE SET NULL;
