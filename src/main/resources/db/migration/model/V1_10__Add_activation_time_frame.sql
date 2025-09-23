CREATE TABLE activation_time_frame (
    id                          BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name                        VARCHAR(255),
    start_time                  TIME NOT NULL,
    stop_time                   TIME NOT NULL,
    exclude_weekends            BOOLEAN NOT NULL
);

ALTER TABLE ad_user ADD CONSTRAINT fk_ad_user_activation_time_frame FOREIGN KEY (activation_time_frame_id) REFERENCES activation_time_frame(id);
