package dk.digitalidentity.OS2ilm.model.dao;

import dk.digitalidentity.OS2ilm.model.entity.AdGroup;
import dk.digitalidentity.OS2ilm.model.entity.Archetype;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdGroupDao extends JpaRepository<AdGroup, Long> {
	@Query("SELECT ag FROM ad_group ag WHERE ag.id NOT IN " +
			"(SELECT aag.id FROM archetype a JOIN a.adGroups aag WHERE a = :archetype)")
	List<AdGroup> findAdGroupsNotLinkedToArchetype(Archetype archetype);

	/**
	 * Find all AdGroups that are not associated with the given Employee.
	 * @param employeeId The employee id
	 * @return A list of AdGroups not associated with the Employee.
	 */
	@Query("SELECT ag FROM ad_group ag WHERE ag NOT IN " +
			"(SELECT adg FROM employee e JOIN e.adGroups adg WHERE e.id = :employeeId)")
	List<AdGroup> findAdGroupsNotLinkedToEmployee(@Param("employeeId") long employeeId);
}
