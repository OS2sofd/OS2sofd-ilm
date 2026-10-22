package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.dao.AdUserDao;
import dk.digitalidentity.OS2ilm.model.entity.ActivationTimeFrame;
import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.enums.SofdSyncStatus;
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
		adUser.setDeleted(true);
		adUserDao.save(adUser);
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

	public List<AdUser> findByStartDateLessThanEqualAndEnabledFalse(LocalDate startDate) {
		return adUserDao.findByStartDateLessThanEqualAndEnabledFalse(startDate);
	}

	public List<AdUser> findByStopDateLessThanEqualAndEnabledTrue(LocalDate stopDate) {
		return adUserDao.findByStopDateLessThanEqualAndEnabledTrue(stopDate);
	}

	public void saveAll(Iterable<AdUser> adUsers) {
		adUserDao.saveAll(adUsers);
	}

	public List<AdUser> findAdUsersByActivationTimeFrame(ActivationTimeFrame activationTimeFrame) {
		return adUserDao.findAdUsersByActivationTimeFrame(activationTimeFrame);
	}

	public List<AdUser> findBySofdSyncStatus(SofdSyncStatus sofdSyncStatus) {
		return adUserDao.findBySofdSyncStatus(sofdSyncStatus);
	}
}
