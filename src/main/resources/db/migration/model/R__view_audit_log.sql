CREATE OR REPLACE VIEW view_audit_log AS
SELECT
    a.id AS id,
    a.tts AS tts,
    a.correlation_id AS correlation_id,
    a.target_id AS target_id,
    a.target_name AS target_name,
    a.performer_id AS performer_id,
    a.performer_name AS performer_name,
    a.message AS message,
    a.log_action AS log_action,
    a.auditlogs_details_id AS detail_id
FROM auditlog a;
