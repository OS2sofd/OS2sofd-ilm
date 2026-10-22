package dk.digitalidentity.OS2ilm.model.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.OS2ilm.model.entity.EmailTemplateAttachmentDeletion;

public interface EmailTemplateAttachmentDeletionDao extends JpaRepository<EmailTemplateAttachmentDeletion, Long> {
	List<EmailTemplateAttachmentDeletion> findByDeleteAfterBefore(LocalDateTime time);
}
