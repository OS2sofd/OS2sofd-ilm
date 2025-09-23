package dk.digitalidentity.OS2ilm.model.dao;

import dk.digitalidentity.OS2ilm.model.entity.OrgUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrgUnitDao extends JpaRepository<OrgUnit, Long> {
	OrgUnit findOrgUnitByUuid(String uuid);
	void deleteByUuidIn(List<String> uuids);
}
