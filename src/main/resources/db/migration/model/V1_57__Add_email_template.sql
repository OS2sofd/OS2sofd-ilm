CREATE TABLE email_template (
    id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    template_type                VARCHAR(64) NOT NULL,
    subject                      VARCHAR(255) NULL,
    content                      TEXT NULL,
    enabled                      BOOLEAN NOT NULL DEFAULT TRUE,
    s3_attachment_key            VARCHAR(255) NULL,
    last_updated                 DATETIME NULL,
    CONSTRAINT uk_email_template_template_type UNIQUE (template_type)
);

-- move the existing CMS mail templates over, converting the old PLACEHOLDER_X tokens to the new {x} tokens.
-- a NULL content or subject is filled in with the default from EmailTemplateType when the template is first read
INSERT IGNORE INTO email_template (template_type, content, enabled, last_updated)
SELECT
    CASE cms_key
        WHEN 'cms.employee.approved'       THEN 'EMPLOYEE_APPROVED'
        WHEN 'cms.employee.created'        THEN 'EMPLOYEE_CREATED'
        WHEN 'cms.employee.extended'       THEN 'EMPLOYEE_EXTENDED'
        WHEN 'cms.employee.closed'         THEN 'EMPLOYEE_CLOSED'
        WHEN 'cms.employee.unlocked'       THEN 'EMPLOYEE_UNLOCKED'
        WHEN 'cms.employee.password.reset' THEN 'EMPLOYEE_PASSWORD_RESET'
        WHEN 'cms.employee.create.request' THEN 'EMPLOYEE_CREATE_REQUEST'
        WHEN 'cms.admin.approved'          THEN 'ADMIN_APPROVED'
        WHEN 'cms.admin.invitation'        THEN 'MAIL_INVITATION'
        WHEN 'cms.awaiting.approval.email' THEN 'AWAITING_APPROVAL'
    END,
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(cms_value,
        'PLACEHOLDER_RECIPIENT', '{modtager}'),
        'PLACEHOLDER_USERNAME', '{brugernavn}'),
        'PLACEHOLDER_PASSWORD', '{kodeord}'),
        'PLACEHOLDER_DATE', '{dato}'),
        'PLACEHOLDER_COUNT', '{antal}'),
        'PLACEHOLDER_LINK', '{link}'),
    active,
    last_updated
FROM cms_message
WHERE cms_key IN (
    'cms.employee.approved', 'cms.employee.created', 'cms.employee.extended', 'cms.employee.closed',
    'cms.employee.unlocked', 'cms.employee.password.reset', 'cms.employee.create.request',
    'cms.admin.approved', 'cms.admin.invitation', 'cms.awaiting.approval.email'
);

-- seed the templates that had no CMS row, so every type exists up front
INSERT IGNORE INTO email_template (template_type) VALUES
    ('EMPLOYEE_APPROVED'), ('EMPLOYEE_CREATED'), ('EMPLOYEE_EXTENDED'), ('EMPLOYEE_CLOSED'), ('EMPLOYEE_UNLOCKED'),
    ('EMPLOYEE_PASSWORD_RESET'), ('EMPLOYEE_CREATE_REQUEST'), ('ADMIN_APPROVED'), ('MAIL_INVITATION'), ('AWAITING_APPROVAL');

-- the awaiting approval header was a separate CMS text, it is now the subject of the template
UPDATE email_template
SET subject = (SELECT cms_value FROM cms_message WHERE cms_key = 'cms.awaiting.approval.email.header' AND cms_value IS NOT NULL LIMIT 1)
WHERE template_type = 'AWAITING_APPROVAL';

DELETE FROM cms_message
WHERE cms_key IN (
    'cms.employee.approved', 'cms.employee.created', 'cms.employee.extended', 'cms.employee.closed',
    'cms.employee.unlocked', 'cms.employee.password.reset', 'cms.employee.create.request',
    'cms.admin.approved', 'cms.admin.invitation', 'cms.awaiting.approval.email', 'cms.awaiting.approval.email.header'
);
