CREATE OR REPLACE VIEW view_active_directory_accounts AS
SELECT
    a.id as id,
    a.name as name,
    a.status as status,
    a.start_date as start_date,
    a.stop_date as stop_date,
    e.id as employee_id,
    GROUP_CONCAT(ag.name ORDER BY ag.name SEPARATOR ', ') as ad_groups
FROM ad_user a
         LEFT JOIN employee e ON e.ad_user_id = a.id
         LEFT JOIN employee_ad_group_mapping eagm ON eagm.employee_id = e.id
         LEFT JOIN ad_group ag ON ag.id = eagm.ad_group_id
WHERE a.deleted = false
GROUP BY a.id, a.name, a.status, a.start_date, a.stop_date, e.id
