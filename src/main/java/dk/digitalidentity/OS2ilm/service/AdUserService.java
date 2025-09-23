package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.dao.AdUserDao;
import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdUserService {
	private final AdUserDao adUserDao;

	public AdUser save(AdUser adUser) {
		return adUserDao.save(adUser);
	}

	public AdUser findById(Long id) {
		return adUserDao.findById(id).orElse(null);
	}

	public void delete(AdUser adUser) {
		adUserDao.delete(adUser);
	}

	public List<AdUser> findAll() {
		return adUserDao.findAll();
	}

	public AdUser findByEmployee(Employee employee) {
		return adUserDao.findByEmployee(employee);
	}

	public AdUser findByName(String name) {
		return adUserDao.findByName(name);
	}

	public List<AdUser> findByNameStartingWith(String prefix) {
		return adUserDao.findByNameStartingWith(prefix);
	}

	public List<AdUser> findByStartDateBeforeAndEnabledFalse(LocalDate startDate) {
		return adUserDao.findByStartDateBeforeAndEnabledFalse(startDate);
	}

	public List<AdUser> findByStopDateBeforeAndEnabledTrue(LocalDate stopDate) {
		return adUserDao.findByStopDateBeforeAndEnabledTrue(stopDate);
	}

	public void saveAll(Iterable<AdUser> adUsers) {
		adUserDao.saveAll(adUsers);
	}

}
