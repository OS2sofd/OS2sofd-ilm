CREATE TABLE leader (
    id                      BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name                    VARCHAR(255),
    mail                    VARCHAR(255),
    uuid                    VARCHAR(36),
    business_id             BIGINT NOT NULL,

    CONSTRAINT fk_leader_business FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE CASCADE
);
