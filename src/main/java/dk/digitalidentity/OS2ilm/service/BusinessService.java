package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.dao.BusinessDao;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.samlmodule.model.TokenUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessService {
    private final BusinessDao businessDao;
    private final EmployeeService employeeService;

	public List<Business> findAll() {
		return businessDao.findAll();
	}

	public List<Business> findAllAndLoaded() {
		return businessDao.findAllAndLoaded();
	}

	public Business findById(Long id) {
		return businessDao.findById(id).orElse(null);
	}

	public Business findByIdAndLoaded(Long id) {
		return businessDao.findByIdAndLoaded(id);
	}

	public Business findByIdWithLeadersLoaded(Long id) {
		return businessDao.findByIdWithLeadersLoaded(id);
	}

	public Business save(Business business) {
		return businessDao.save(business);
	}

	@Transactional
	public Business updateCvr(Business business, String newCvr) {
		business.setCvr(newCvr);
		employeeService.updateCvrForEmployees(business, newCvr);
		return businessDao.save(business);
	}

	@Transactional
	public Business findBusinessByCvr(String cvr) {
		return businessDao.findBusinessByCvr(cvr);
	}

	@Transactional
	public Business findBusinessByCvrAndLoaded(String cvr) {
		return businessDao.findBusinessByCvrAndLoaded(cvr);
	}

	public void delete(Business business) {
		businessDao.delete(business);
	}

	public void saveAll(List<Business> businesses) {
		businessDao.saveAll(businesses);
	}

	public List<Business> findByCvrIn(List<String> cvrs) {
		return businessDao.findByCvrIn(cvrs);
	}

	public Boolean doesBusinessExist(TokenUser tokenUser) {
		boolean exists = false;
		Business business = businessDao.findBusinessByCvr(tokenUser.getCvr());
		if (business != null) {
			exists = true;
		}
		return exists;
	}

	public boolean checkLeaderEnabled(String cvr) {
		if (cvr == null) {
			return false;
		}
		Business business =  businessDao.findBusinessByCvr(cvr);
		if (business == null) {
			return false;
		}

		return business.isEnableLeaders();
	}
}
