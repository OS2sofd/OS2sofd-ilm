CREATE TABLE employee_ad_group_mapping_aud (
    id                          BIGINT NOT NULL AUTO_INCREMENT,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    employee_id                 BIGINT NOT NULL,
    ad_group_id                 BIGINT NOT NULL,

    FOREIGN KEY fk_employee_ad_group_mapping_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_employee_ad_group_mapping_aud(id, rev)
);

CREATE TABLE employee_role_group_mapping_aud (
    id                          BIGINT NOT NULL AUTO_INCREMENT,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    employee_id                 BIGINT NOT NULL,
    role_group_id               BIGINT NOT NULL,

    FOREIGN KEY fk_employee_role_group_mapping_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_employee_role_group_mapping_aud(id, rev)
);

CREATE TABLE employee_user_role_mapping_aud (
    id                          BIGINT NOT NULL AUTO_INCREMENT,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    employee_id                 BIGINT NOT NULL,
    user_role_id                BIGINT NOT NULL,

    FOREIGN KEY fk_employee_user_role_mapping_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_employee_user_role_mapping_aud(id, rev)
);

CREATE TABLE business_archetypes_aud (
    id                          BIGINT NOT NULL AUTO_INCREMENT,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    business_id                 BIGINT NOT NULL,
    archetype_id                BIGINT NOT NULL,

    FOREIGN KEY fk_business_archetypes_aud_rev(rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_business_archetypes_aud(id, rev)
);
