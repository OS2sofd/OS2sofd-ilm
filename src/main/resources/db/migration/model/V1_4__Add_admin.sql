CREATE TABLE admin (
    id                         BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name                       VARCHAR(255),
    uuid                       VARCHAR(255),
    email                      VARCHAR(255),
    role                       VARCHAR(255)
);

CREATE TABLE admin_aud (
    id                          BIGINT NOT NULL,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    name                        VARCHAR(255),
    uuid                        VARCHAR(255),
    email                       VARCHAR(255),

    FOREIGN KEY fk_admin_aud_rev(rev) REFERENCES revinfo (id),
    PRIMARY KEY pk_admin_aud(id, rev)
);