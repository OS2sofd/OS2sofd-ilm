ALTER TABLE employee_ad_group_mapping
DROP FOREIGN KEY fk_employee_ad_group;

ALTER TABLE employee_ad_group_mapping
ADD CONSTRAINT fk_employee_ad_group FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE;

ALTER TABLE employee_ad_group_mapping
DROP FOREIGN KEY fk_ad_group_employee;

ALTER TABLE employee_ad_group_mapping
ADD CONSTRAINT fk_ad_group_employee FOREIGN KEY (ad_group_id) REFERENCES ad_group(id) ON DELETE CASCADE;


ALTER TABLE employee_user_role_mapping
DROP FOREIGN KEY fk_employee_user_role;

ALTER TABLE employee_user_role_mapping
ADD CONSTRAINT fk_employee_user_role FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE;

ALTER TABLE employee_user_role_mapping
DROP FOREIGN KEY fk_user_role_employee;

ALTER TABLE employee_user_role_mapping
ADD CONSTRAINT fk_user_role_employee FOREIGN KEY (user_role_id) REFERENCES user_role(id) ON DELETE CASCADE;
