ALTER TABLE employee ADD CONSTRAINT unique_employee_cpr UNIQUE (cpr);
ALTER TABLE employee ADD CONSTRAINT unique_employee_mit_id_uuid UNIQUE (mit_id_uuid);
