package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.dao.RoleGroupDao;
import dk.digitalidentity.OS2ilm.model.entity.RoleGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleGroupService {
	private final RoleGroupDao roleGroupDao;

	public List<RoleGroup> findAll() {
		return roleGroupDao.findAll();
	}

	public RoleGroup save(RoleGroup roleGroup) {
		return roleGroupDao.save(roleGroup);
	}

	public void deleteAll(List<RoleGroup> roleGroups) {
		roleGroupDao.deleteAll(roleGroups);
	}

	public RoleGroup findById(Long id) {
		return roleGroupDao.findById(id).orElse(null);
	}
}
