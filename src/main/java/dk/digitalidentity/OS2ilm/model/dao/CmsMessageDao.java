package dk.digitalidentity.OS2ilm.model.dao;

import dk.digitalidentity.OS2ilm.model.entity.CmsMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CmsMessageDao extends JpaRepository<CmsMessage, Long> {
	List<CmsMessage> findAll();
	List<CmsMessage> findAllByLastUpdatedAfter(LocalDateTime after);
	CmsMessage findByCmsKey(String cmsKey);
}
