CREATE TABLE business_employment_settings
(
    id                      BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    business_id             BIGINT NOT NULL,
    max_future_days         BIGINT NULL,
    max_employment_days     BIGINT NULL,
    default_employment_days BIGINT NULL,
    created_at              DATETIME,
    updated_at              DATETIME,

    CONSTRAINT fk_business_employment_settings FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE CASCADE
);
