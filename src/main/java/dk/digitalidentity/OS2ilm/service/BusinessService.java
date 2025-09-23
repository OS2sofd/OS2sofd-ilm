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

	public List<Business> findAll() {
		return businessDao.findAll();
	}

	public Business findById(Long id) {
		return businessDao.findById(id).orElse(null);
	}

	public Business save(Business business) {
		return businessDao.save(business);
	}

	@Transactional
	public Business findBusinessByCvr(String cvr) {
		return businessDao.findBusinessByCvr(cvr);
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

}
