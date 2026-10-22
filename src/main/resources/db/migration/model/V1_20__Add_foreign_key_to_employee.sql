ALTER TABLE employee
    ADD CONSTRAINT fk_employee_activation_time_frame FOREIGN KEY (activation_time_frame_id) REFERENCES activation_time_frame(id) ON DELETE SET NULL;
