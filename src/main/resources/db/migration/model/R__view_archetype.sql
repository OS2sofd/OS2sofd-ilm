CREATE OR REPLACE VIEW view_archetype AS
SELECT
    a.id AS id,
    a.name AS name
FROM archetype a;
