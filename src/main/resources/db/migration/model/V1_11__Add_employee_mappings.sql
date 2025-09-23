CREATE TABLE employee_ad_group_mapping (
    employee_id               BIGINT NOT NULL,
    ad_group_id                 BIGINT NOT NULL,

    FOREIGN KEY fk_employee_ad_group (employee_id) REFERENCES employee(id),
    FOREIGN KEY fk_ad_group_employee (ad_group_id) REFERENCES ad_group(id),
    PRIMARY KEY pk_employee_ad_group (employee_id, ad_group_id)
);

CREATE TABLE employee_user_role_mapping (
    employee_id                BIGINT NOT NULL,
    user_role_id               BIGINT NOT NULL,

    FOREIGN KEY fk_employee_user_role (employee_id) REFERENCES employee(id),
    FOREIGN KEY fk_user_role_employee (user_role_id) REFERENCES user_role(id),
    PRIMARY KEY pk_employee_user_role (employee_id, user_role_id)
);