package dk.digitalidentity.OS2ilm.model.dao;

import dk.digitalidentity.OS2ilm.model.entity.EmailTemplate;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplateType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTemplateDao extends JpaRepository<EmailTemplate, Long> {
	EmailTemplate findByTemplateType(EmailTemplateType templateType);
	boolean existsByS3AttachmentKey(String s3AttachmentKey);
}
