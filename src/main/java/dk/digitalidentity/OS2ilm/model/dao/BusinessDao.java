package dk.digitalidentity.OS2ilm.model.dao;

import dk.digitalidentity.OS2ilm.model.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BusinessDao extends JpaRepository<Business, Long> {

	Business findBusinessByCvr(String cvr);

    List<Business> findByCvrIn(Collection<String> cvrs);
}
