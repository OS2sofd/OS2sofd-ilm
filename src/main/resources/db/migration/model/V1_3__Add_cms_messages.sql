CREATE TABLE cms_message (
    id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    cms_key                      VARCHAR(255) NOT NULL,
    cms_value                    TEXT,
    last_updated                 DATETIME NULL
);