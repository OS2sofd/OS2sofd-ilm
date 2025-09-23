CREATE TABLE archetype (
    id                          BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name                        VARCHAR(255)
);

CREATE TABLE archetype_user_role_mapping (
    archetype_id                BIGINT NOT NULL,
    user_role_id                BIGINT NOT NULL,

    FOREIGN KEY fk_archetype_user_role_ (archetype_id) REFERENCES archetype(id),
    FOREIGN KEY fk_user_role_archetype (user_role_id) REFERENCES user_role(id),
    PRIMARY KEY pk_archetype_user_role(archetype_id, user_role_id)
);