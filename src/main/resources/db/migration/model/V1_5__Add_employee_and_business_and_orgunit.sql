CREATE TABLE orgunit (
    uuid                        VARCHAR(36) NOT NULL PRIMARY KEY,
    name                        VARCHAR(255) NOT NULL,
    parent_uuid                 VARCHAR(36) NULL
);

CREATE TABLE business (
    id                          BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name                        VARCHAR(255),
    cvr                         VARCHAR(255),
    prefix                      VARCHAR(255)
);

CREATE TABLE employee (
    id                          BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name                        VARCHAR(255),
    mit_id_uuid                 VARCHAR(255),
    cpr                         VARCHAR(10),
    email                       VARCHAR(255),
    phone                       BIGINT DEFAULT NULL,
    business_id                 BIGINT,
    approved                    VARCHAR(255) NOT NULL,
    integrated                  VARCHAR(255),
    cvr                         VARCHAR(255),
    accepted_tos                BOOLEAN DEFAULT FALSE,
    ad_user_id                  BIGINT,
    blocked                     BOOLEAN DEFAULT NULL,
    org_unit_uuid               VARCHAR(255),
    start_date                  DATE,
    stop_date                   DATE,
    position_name               VARCHAR(255) NOT NULL,
    last_login                  DATETIME DEFAULT NULL,
    role                        VARCHAR(255),
    activation_time_frame_id    BIGINT,

    FOREIGN KEY fk_employees_business (business_id) REFERENCES business (id) ON DELETE CASCADE,
    FOREIGN KEY fk_employees_orgunit (org_unit_uuid) REFERENCES orgunit (uuid) ON DELETE SET NULL
);

CREATE TABLE employee_aud (
    id                          BIGINT NOT NULL,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    name                        VARCHAR(255),
    mit_id_uuid                 VARCHAR(255),
    cpr                         VARCHAR(10),
    email                       VARCHAR(255),
    phone                       BIGINT DEFAULT NULL,
    approved                    VARCHAR(255) NOT NULL,
    cvr                         VARCHAR(10),
    accepted_tos                BOOLEAN DEFAULT NULL,
    ad_user_id                  BIGINT  DEFAULT NULL,
    blocked                     BOOLEAN DEFAULT NULL,
    start_date                  DATE NOT NULL,
    stop_date                   DATE,
    last_login                  DATETIME DEFAULT NULL,

    FOREIGN KEY fk_employee_aud_rev (rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_employee_aud(id, rev)
);

