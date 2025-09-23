package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.dao.ArchetypeDao;
import dk.digitalidentity.OS2ilm.model.entity.Archetype;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArchetypeService {
	private final ArchetypeDao archetypeDao;

	public List<Archetype> findAll() {
		return archetypeDao.findAll();
	}

	public void delete(Archetype archetype) {
		archetypeDao.delete(archetype);
	}

	public Archetype findById(@PathVariable long id) {
		return archetypeDao.findById(id).orElse(null);
	}

	public Archetype save(Archetype archetype) {
		return archetypeDao.save(archetype);
	}

	public void saveAll(List<Archetype> archetypes) {
		archetypeDao.saveAll(archetypes);
	}

	public Archetype findByIdWithUserRolesAndAdGroups(long id) {
		return archetypeDao.findByIdWithUserRolesAndAdGroups(id);
	}
}
