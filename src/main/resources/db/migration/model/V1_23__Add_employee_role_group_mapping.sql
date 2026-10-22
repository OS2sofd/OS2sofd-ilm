CREATE TABLE employee_role_group_mapping (
    employee_id                BIGINT NOT NULL,
    role_group_id               BIGINT NOT NULL,

    FOREIGN KEY fk_employee_role_group (employee_id) REFERENCES employee(id) ON DELETE CASCADE,
    FOREIGN KEY fk_role_group_employee (role_group_id) REFERENCES role_group(id) ON DELETE CASCADE,
    PRIMARY KEY pk_employee_role_group(employee_id, role_group_id)
);
