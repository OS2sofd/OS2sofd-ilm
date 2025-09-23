CREATE TABLE auditlog_detail (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  detail_type                  VARCHAR(64) NOT NULL,
  detail_content               TEXT,
  detail_supplement            TEXT
);

CREATE TABLE auditlog (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  
  -- metadata
  tts                          DATETIME NULL,
  ip_address                   VARCHAR(255),
  correlation_id               VARCHAR(255),

  -- referenced person
  target_id                    BIGINT NULL,
  target_name                  VARCHAR(255),
  
  -- referenced performer
  performer_id                 VARCHAR(255),
  performer_name               VARCHAR(255),

  -- structured log-data
  log_action                   VARCHAR(64) NOT NULL,
  log_target_id                VARCHAR(255),
  log_target_name              VARCHAR(255),
  
  -- abbreviated human-readable message
  message                      VARCHAR(500),
  
  -- full details (lazy-loaded by code)
  auditlogs_details_id         BIGINT,
  location                     VARCHAR(255),
  
  FOREIGN KEY fk_auditlog_detail (auditlogs_details_id) REFERENCES auditlog_detail(id),
  INDEX(tts)
);
