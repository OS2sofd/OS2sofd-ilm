CREATE OR REPLACE VIEW view_approved_employees AS
SELECT
    e.id AS id,
    e.name AS name,
    CONCAT(SUBSTRING(e.cpr, 1, (CHAR_LENGTH(e.cpr) - 4)), '-xxxx') AS cpr,
    e.cvr AS cvr,
    e.approved AS approval
FROM employee e
WHERE e.approved IN ('AWAITING_APPROVAL', 'LEADER_APPROVED', 'AWAITING_EXTENSION_APPROVAL');