package dk.digitalidentity.OS2ilm.model.dao;

import dk.digitalidentity.OS2ilm.model.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleDao extends JpaRepository<UserRole, Long> {
	/**
	 * Find all UserRoles that are not associated with the given Archetype.
	 * @param archetypeId The archetype id
	 * @return A list of UserRoles not associated with the Archetype
	 */
	@Query("SELECT ur FROM UserRole ur WHERE ur NOT IN " +
			"(SELECT ura FROM archetype a JOIN a.userRoles ura WHERE a.id = :archetypeId)")
	List<UserRole> findUserRolesNotLinkedToArchetype(@Param("archetypeId") long archetypeId);

	/**
	 * Find all UserRoles that are not associated with the given Employee.
	 * @param employeeId The employee id
	 * @return A list of UserRoles not associated with the Employee
	 */
	@Query("SELECT ur FROM UserRole ur WHERE ur NOT IN " +
			"(SELECT urc FROM employee e JOIN e.userRoles urc WHERE e.id = :employeeId)")
	List<UserRole> findUserRolesNotLinkedToEmployee(@Param("employeeId") long employeeId);
}
