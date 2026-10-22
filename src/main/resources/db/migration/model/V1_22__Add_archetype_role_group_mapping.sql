CREATE TABLE archetype_role_group_mapping (
    archetype_id                BIGINT NOT NULL,
    role_group_id               BIGINT NOT NULL,

    FOREIGN KEY fk_archetype_role_group (archetype_id) REFERENCES archetype(id) ON DELETE CASCADE,
    FOREIGN KEY fk_role_group_archetype (role_group_id) REFERENCES role_group(id) ON DELETE CASCADE,
    PRIMARY KEY pk_archetype_role_group(archetype_id, role_group_id)
);
