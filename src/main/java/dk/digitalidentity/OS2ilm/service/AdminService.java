package dk.digitalidentity.OS2ilm.service;

import java.util.List;

import dk.digitalidentity.OS2ilm.model.dao.AdminDao;
import dk.digitalidentity.OS2ilm.model.entity.Admin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {
	private final AdminDao adminDao;

	public List<Admin> findAll() {
		return adminDao.findAll();
	}

	public Admin save(Admin admin) {
		return adminDao.save(admin);
	}

	public Admin findById(Long id) {
		return adminDao.findById(id).orElse(null);
	}

	public void saveAll(List<Admin> admins) {
		adminDao.saveAll(admins);
	}

	public Admin findByUuid(String uuid) {
		return adminDao.findByUuid(uuid);
	}
}
