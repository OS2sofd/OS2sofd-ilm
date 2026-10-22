-- email template attachments that are no longer used by a template. They are deleted from S3 once delete_after has passed,
-- because queued e-boks messages only store the S3 key and read the file when they are sent
CREATE TABLE email_template_attachment_deletion (
    id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    s3_key                       VARCHAR(255) NOT NULL,
    delete_after                 DATETIME NOT NULL
);
