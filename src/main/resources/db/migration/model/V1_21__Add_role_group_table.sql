CREATE TABLE role_group (
    id                      BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    role_group_id           BIGINT NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    description             TEXT
);
