package dk.digitalidentity.OS2ilm.model.dao;

import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AdUserDao extends JpaRepository<AdUser, Long> {
    AdUser findByEmployee(Employee employee);

	AdUser findByName(String name);

	List<AdUser> findByNameStartingWith(String prefix);

	List<AdUser> findByStartDateBeforeAndEnabledFalse(LocalDate startDate);

	List<AdUser> findByStopDateBeforeAndEnabledTrue(LocalDate stopDate);
}
