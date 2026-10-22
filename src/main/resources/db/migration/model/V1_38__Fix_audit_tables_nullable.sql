-- Envers stores NULL for all non-PK columns in entity DEL revisions (revtype=2).
-- Non-PK columns in entity audit tables must be nullable to support this.
-- Note: ManyToMany collection audit tables (archetype_*_mapping_aud) are fine
-- because Envers always stores actual FK values for collection entries.

ALTER TABLE business_employment_settings_aud MODIFY business_id BIGINT NULL;

ALTER TABLE user_role_aud MODIFY user_role_id BIGINT NULL;

ALTER TABLE role_group_aud MODIFY role_group_id BIGINT NULL;