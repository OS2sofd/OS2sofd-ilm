package dk.digitalidentity.OS2ilm.model.dao;

import dk.digitalidentity.OS2ilm.model.entity.Archetype;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArchetypeDao extends JpaRepository<Archetype, Long> {
	/**
	 * Find the archetype with the given id and load the UserRoles and AdGroups to avoid no session errors.
	 * @param id The archetype id
	 * @return An archetype with the AdGroups and UserRoles loaded.
	 */
	@Query("SELECT a FROM archetype a " +
			"LEFT JOIN FETCH a.userRoles " +
			"LEFT JOIN FETCH a.adGroups " +
			"LEFT JOIN FETCH a.roleGroups " +
			"WHERE a.id = :id")
	Archetype findByIdWithUserRolesAndAdGroupsAndRoleGroups(@Param("id") long id);
}
