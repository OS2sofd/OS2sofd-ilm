package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.dao.UserRoleDao;
import dk.digitalidentity.OS2ilm.model.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRoleService {
	private final UserRoleDao userRoleDao;

	public List<UserRole> findAll() {
		return userRoleDao.findAll();
	}

	public UserRole findById(long id) {
		return userRoleDao.findById(id).orElse(null);
	}

	public List<UserRole> findUserRolesNotLinkedToArchetype(long archetypeId) {
		return userRoleDao.findUserRolesNotLinkedToArchetype(archetypeId);
	}

	public UserRole save(UserRole userRole) {
		return userRoleDao.save(userRole);
	}

	public List<UserRole> findUserRolesNotLinkedToEmployee(Long employeeId) {
		return userRoleDao.findUserRolesNotLinkedToEmployee(employeeId);
	}
}
