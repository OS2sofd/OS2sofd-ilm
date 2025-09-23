package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.dao.AdGroupDao;
import dk.digitalidentity.OS2ilm.model.entity.AdGroup;
import dk.digitalidentity.OS2ilm.model.entity.Archetype;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdGroupService {
	private final AdGroupDao adGroupDao;

	public AdGroup save(AdGroup adGroup) {
		return adGroupDao.save(adGroup);
	}

	public AdGroup findById(Long id) {
		return adGroupDao.findById(id).orElse(null);
	}

	public void delete(AdGroup adGroup) {
		adGroupDao.delete(adGroup);
	}

	public void deleteAll(Iterable<AdGroup> adGroups) {
		adGroupDao.deleteAll(adGroups);
	}

	public List<AdGroup> findAll() {
		return adGroupDao.findAll();
	}

	public List<AdGroup> saveAll(List<AdGroup> groups) {
		return adGroupDao.saveAll(groups);
	}

	public List<AdGroup> findAdGroupsNotLinkedToArchetype(Archetype archetype) {
		return adGroupDao.findAdGroupsNotLinkedToArchetype(archetype);
	}

	public List<AdGroup> findAdGroupsNotLinkedToEmployee(long employeeId) {
		return adGroupDao.findAdGroupsNotLinkedToEmployee(employeeId);
	}
}
