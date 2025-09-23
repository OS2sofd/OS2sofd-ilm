CREATE TABLE it_system (
    id                      BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name                    VARCHAR(255) NOT NULL,
    it_system_id            BIGINT NOT NULL
);

CREATE TABLE user_role (
    id                      BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_role_id            BIGINT NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    description             TEXT,
    it_system_id            BIGINT NOT NULL,

    CONSTRAINT fk_user_roles_it_systems FOREIGN KEY (it_system_id) REFERENCES it_system(id) ON DELETE CASCADE
);

