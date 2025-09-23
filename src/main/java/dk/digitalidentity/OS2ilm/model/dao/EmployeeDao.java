package dk.digitalidentity.OS2ilm.model.dao;

import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.OrgUnit;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public interface EmployeeDao extends JpaRepository<Employee, Long> {
	List<Employee> findAllEmployeesByBusiness(Business business);
	Employee findByCpr(String cpr);
	Employee findByMitIdUuid(String mitIdUuid);
    List<Employee> findByCvr(String cvr);
	List<Employee> findByOrgUnit(OrgUnit orgUnit);
	List<Employee> findByBusinessAndRole(Business business, String role);


	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM employee_aud WHERE id IN ?1 LIMIT 5000")
	void deleteInAud(ArrayList<Long> ids);

	List<Employee> findByLastLoginBefore(LocalDateTime lastLoginBefore);



	@Query("SELECT e FROM employee e " +
			"LEFT JOIN FETCH e.userRoles " +
			"WHERE e.integrated = :status")
	List<Employee> findByIntegratedAndLoaded(@Param("status") EmployeeStatus status);


	/**
	 * Find the employee with the given id and load the UserRoles and AdGroups to avoid no session errors.
	 * @param id The employee id
	 * @return An employee with the AdGroups and UserRoles loaded.
	 */
	@Query("SELECT e FROM employee e " +
			"LEFT JOIN FETCH e.userRoles " +
			"LEFT JOIN FETCH e.adGroups " +
			"WHERE e.id = :id")
	Employee findByIdWithUserRolesAndAdGroups(@Param("id") long id);
}
