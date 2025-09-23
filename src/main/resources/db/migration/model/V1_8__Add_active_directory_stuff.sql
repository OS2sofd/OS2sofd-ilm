CREATE TABLE ad_group (
    id                          BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name                        VARCHAR(255) NOT NULL
);

CREATE TABLE ad_user
(
    id                          BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name                        VARCHAR(255) NOT NULL,
    start_date                  DATETIME     NOT NULL,
    stop_date                   DATETIME     NOT NULL,
    ad_group_id                 BIGINT,
    status                      VARCHAR(255),
    activation_time_frame_id    BIGINT,
    enabled                     BOOLEAN
);

CREATE TABLE ad_user_ad_group_mapping (
    ad_user_id                  BIGINT NOT NULL,
    ad_group_id                 BIGINT NOT NULL,

    FOREIGN KEY fk_ad_user_ad_group (ad_user_id) REFERENCES ad_user(id),
    FOREIGN KEY fk_ad_group_ad_user (ad_group_id) REFERENCES ad_group(id),
    PRIMARY KEY pk_ad_user_ad_group (ad_user_id, ad_group_id)
);

CREATE TABLE archetype_ad_group_mapping (
    archetype_id                 BIGINT NOT NULL,
    ad_group_id                  BIGINT NOT NULL,

    FOREIGN KEY fk_archetype_ad_group (archetype_id) REFERENCES archetype(id),
    FOREIGN KEY fk_ad_group_archetype (ad_group_id) REFERENCES ad_group(id),
    PRIMARY KEY pk_archetype_ad_group (archetype_id, ad_group_id)
);

ALTER TABLE employee ADD CONSTRAINT fk_employees_ad_user_id FOREIGN KEY (ad_user_id) REFERENCES ad_user(id);