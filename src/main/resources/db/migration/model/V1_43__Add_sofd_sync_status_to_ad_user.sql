ALTER TABLE ad_user ADD COLUMN sofd_sync_status VARCHAR(255);
UPDATE ad_user SET sofd_sync_status = 'CREATED' WHERE sofd_sync_status IS NULL;
