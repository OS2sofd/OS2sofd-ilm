ALTER TABLE employee_aud ADD (
    integrated      VARCHAR(255),
    business_id     BIGINT,
    position_name   VARCHAR(255),
    role            VARCHAR(255),
    org_unit_uuid   VARCHAR(64)
);

ALTER TABLE employee_aud DROP COLUMN start_date;
ALTER TABLE employee_aud ADD COLUMN start_date DATE DEFAULT NULL;

CREATE TABLE business_aud (
    id                          BIGINT NOT NULL,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    name                        VARCHAR(255),
    cvr                         VARCHAR(255),
    prefix                      VARCHAR(10),
    enable_leaders              BOOLEAN,
    inherit_privileges          BOOLEAN,
    default_org_unit_uuid       VARCHAR(64),
    activation_time_frame_id    BIGINT,

    FOREIGN KEY fk_business_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_business_aud(id, rev)
);

CREATE TABLE leader_aud (
    id                          BIGINT NOT NULL,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    uuid                        VARCHAR(64),
    name                        VARCHAR(255),
    mail                        VARCHAR(255),
    business_id                 BIGINT,

    FOREIGN KEY fk_leader_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_leader_aud(id, rev)
);

CREATE TABLE ad_user_aud (
    id                          BIGINT NOT NULL,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    name                        VARCHAR(255),
    start_date                  DATE,
    stop_date                   DATE,
    status                      VARCHAR(255),
    enabled                     BOOLEAN,
    ext_uuid                    VARCHAR(64),

    FOREIGN KEY fk_ad_user_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_ad_user_aud(id, rev)
);

CREATE TABLE activation_time_frame_aud (
    id                          BIGINT NOT NULL,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    name                        VARCHAR(255),
    start_time                  TIME,
    stop_time                   TIME,
    exclude_weekends            BOOLEAN,

    FOREIGN KEY fk_activation_time_frame_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_activation_time_frame_aud(id, rev)
);

CREATE TABLE archetype_aud (
    id                          BIGINT NOT NULL,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    name                        VARCHAR(255),

    FOREIGN KEY fk_archetype_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_archetype_aud(id, rev)
);

CREATE TABLE archetype_user_role_mapping_aud (
    id                          BIGINT NOT NULL AUTO_INCREMENT,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    archetype_id                BIGINT NOT NULL,
    user_role_id                BIGINT NOT NULL,

    FOREIGN KEY fk_archetype_user_roles_mapping_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_archetype_user_roles_mapping_aud(id, rev)
);

CREATE TABLE archetype_ad_group_mapping_aud (
    id                          BIGINT NOT NULL AUTO_INCREMENT,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    archetype_id                BIGINT NOT NULL,
    ad_group_id                 BIGINT NOT NULL,

    FOREIGN KEY fk_archetype_ad_group_mapping_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_archetype_ad_group_mapping_aud(id, rev)
);

CREATE TABLE archetype_role_group_mapping_aud (
    id                          BIGINT NOT NULL AUTO_INCREMENT,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    archetype_id                BIGINT NOT NULL,
    role_group_id               BIGINT NOT NULL,

    FOREIGN KEY fk_archetype_role_group_mapping_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_archetype_role_group_mapping_aud(id, rev)
);

CREATE TABLE user_role_aud (
    id                          BIGINT NOT NULL,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    user_role_id                BIGINT NOT NULL,
    name                        VARCHAR(255),
    description                 TEXT,

    FOREIGN KEY fk_user_roles_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_user_roles_aud(id, rev)
);

CREATE TABLE role_group_aud (
    id                          BIGINT NOT NULL,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    role_group_id               BIGINT NOT NULL,
    name                        VARCHAR(255),
    description                 TEXT,

    FOREIGN KEY fk_role_group_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_role_group_aud(id, rev)
);

CREATE TABLE orgunit_aud (
    uuid                        VARCHAR(64) NOT NULL,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    name                        VARCHAR(255),
    parent_uuid                 VARCHAR(64),

    FOREIGN KEY fk_orgunit_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_orgunit_aud(uuid, rev)
);

CREATE TABLE business_employment_settings_aud (
    id                          BIGINT NOT NULL,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    business_id                 BIGINT NOT NULL,
    max_future_days             BIGINT,
    max_employment_days         BIGINT,
    default_employment_days     BIGINT,
    created_at                  DATETIME,
    updated_at                  DATETIME,

    FOREIGN KEY fk_business_employment_settings_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_business_employment_settings_aud(id, rev)
);

CREATE TABLE ad_group_aud (
    id                          BIGINT NOT NULL,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    name                        VARCHAR(255),

    FOREIGN KEY fk_ad_group_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_ad_group_aud(id, rev)
);
