package dk.digitalidentity.OS2ilm.service;

import java.util.List;

import dk.digitalidentity.OS2ilm.model.dao.ItSystemDao;
import dk.digitalidentity.OS2ilm.model.entity.ItSystem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItSystemService {
	private final ItSystemDao itSystemDao;

	public List<ItSystem> findAll() {
		return itSystemDao.findAll();
	}

	public ItSystem save(ItSystem entity) {
		return itSystemDao.save(entity);
	}

	public void delete(ItSystem entity) {
		itSystemDao.delete(entity);
	}

	public void deleteAll(List<ItSystem> entities) {
		itSystemDao.deleteAll(entities);
	}
}
