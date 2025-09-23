package dk.digitalidentity.OS2ilm.model.dao;

import dk.digitalidentity.OS2ilm.model.entity.ItSystem;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ItSystemDao extends CrudRepository<ItSystem, Long> {
	List<ItSystem> findAll();
}
