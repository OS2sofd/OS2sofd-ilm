CREATE OR REPLACE VIEW view_employee AS
SELECT
    e.id AS id,
    e.name AS name,
    e.cvr AS cvr,
    ou.name AS org_unit_name,
    e.start_date AS start_date,
    e.stop_date AS stop_date,
    e.approved AS approved,
    e.integrated AS integrated,
    bs.name AS business_name,
    bs.id AS business_id,
    au.name AS sam_account_name,
    e.accepted_tos AS accepted_tos,
    e.exchange_account_status AS exchange_account_status
FROM employee e
    LEFT JOIN orgunit ou on e.org_unit_uuid = ou.uuid
    LEFT JOIN business bs on bs.cvr = e.cvr
    LEFT JOIN ad_user au on au.id = e.ad_user_id
WHERE e.deleted = false
    AND e.approved <> 'DENIED'
GROUP BY e.id
