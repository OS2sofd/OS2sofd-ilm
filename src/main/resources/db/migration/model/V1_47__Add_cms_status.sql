ALTER TABLE cms_message ADD COLUMN cms_status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE';
UPDATE cms_message SET cms_status = 'INACTIVE' WHERE active = false;
