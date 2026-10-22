ALTER TABLE archetype_user_role_mapping
    DROP FOREIGN KEY fk_archetype_user_role_;

ALTER TABLE archetype_user_role_mapping
    ADD CONSTRAINT fk_archetype_user_role FOREIGN KEY (archetype_id) REFERENCES archetype(id) ON DELETE CASCADE;

ALTER TABLE archetype_user_role_mapping
    DROP FOREIGN KEY fk_user_role_archetype;

ALTER TABLE archetype_user_role_mapping
    ADD CONSTRAINT fk_user_role_archetype FOREIGN KEY (user_role_id) REFERENCES user_role(id) ON DELETE CASCADE;
