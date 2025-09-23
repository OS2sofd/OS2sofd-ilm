package dk.digitalidentity.OS2ilm.model.dao;

import dk.digitalidentity.OS2ilm.model.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminDao extends JpaRepository<Admin, Long> {
    Admin findByUuid(String uuid);
}
