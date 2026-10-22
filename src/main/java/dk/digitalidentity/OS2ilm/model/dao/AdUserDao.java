package dk.digitalidentity.OS2ilm.model.dao;

import dk.digitalidentity.OS2ilm.model.entity.ActivationTimeFrame;
import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.enums.SofdSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AdUserDao extends JpaRepository<AdUser, Long> {
    AdUser findByEmployee(Employee employee);

	AdUser findByName(String name);

	List<AdUser> findByNameStartingWith(String prefix);

	@Query("SELECT u FROM ad_user u " +
			"LEFT JOIN FETCH u.activationTimeFrame atf " +
			"WHERE u.startDate <= :startDate AND u.enabled = false")
	List<AdUser> findByStartDateLessThanEqualAndEnabledFalse(@Param("startDate") LocalDate startDate);

	@Query("SELECT u FROM ad_user u " +
			"LEFT JOIN FETCH u.activationTimeFrame atf " +
			"WHERE u.stopDate <= :stopDate AND u.enabled = true")
	List<AdUser> findByStopDateLessThanEqualAndEnabledTrue(@Param("stopDate") LocalDate stopDate);

	List<AdUser> findAdUsersByActivationTimeFrame(ActivationTimeFrame activationTimeFrame);

	List<AdUser> findBySofdSyncStatus(SofdSyncStatus sofdSyncStatus);
}
