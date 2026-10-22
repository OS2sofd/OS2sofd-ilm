ALTER TABLE business
    ADD COLUMN activation_time_frame_id BIGINT;

ALTER TABLE business
    ADD CONSTRAINT fk_business_activation_time_frame FOREIGN KEY (activation_time_frame_id) REFERENCES activation_time_frame(id) ON DELETE SET NULL;
