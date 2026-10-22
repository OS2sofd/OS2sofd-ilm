package dk.digitalidentity.OS2ilm.model.dao;

import dk.digitalidentity.OS2ilm.model.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BusinessDao extends JpaRepository<Business, Long> {

	Business findBusinessByCvr(String cvr);

    List<Business> findByCvrIn(Collection<String> cvrs);

	@Query("SELECT b FROM business b " +
			"LEFT JOIN FETCH b.defaultOrgUnit " +
			"LEFT JOIN FETCH b.selectableArchetypes " +
			"LEFT JOIN FETCH b.businessEmploymentSettings"
	)
	List<Business> findAllAndLoaded();

	@Query("SELECT b FROM business b " +
			"LEFT JOIN FETCH b.defaultOrgUnit " +
			"LEFT JOIN FETCH b.businessEmploymentSettings " +
			"LEFT JOIN FETCH b.selectableArchetypes " +
			"WHERE b.id = :id")
	Business findByIdAndLoaded(@Param("id") Long id);

	@Query("SELECT b FROM business b " +
			"LEFT JOIN FETCH b.defaultOrgUnit " +
			"LEFT JOIN FETCH b.businessEmploymentSettings " +
			"WHERE b.cvr = :cvr")
	Business findBusinessByCvrAndLoaded(@Param("cvr") String cvr);

	@Query("SELECT b FROM business b " +
			"LEFT JOIN FETCH b.leaders " +
			"WHERE b.id = :id")
	Business findByIdWithLeadersLoaded(Long id);
}
