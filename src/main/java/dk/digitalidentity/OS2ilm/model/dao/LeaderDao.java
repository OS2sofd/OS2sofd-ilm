package dk.digitalidentity.OS2ilm.model.dao;

import dk.digitalidentity.OS2ilm.model.entity.Leader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LeaderDao extends JpaRepository<Leader, Long> {
    Leader findByMitIdUuid(String mitIdUuid);

    @Query("SELECT l FROM leader l LEFT JOIN FETCH l.businesses WHERE l.mitIdUuid = :mitIdUuid")
    Leader findByMitIdUuidWithBusinesses(String mitIdUuid);

    Leader findByCpr(String cpr);

    @Query("SELECT l FROM leader l LEFT JOIN FETCH l.businesses WHERE l.cpr = :cpr")
    Leader findByCprWithBusinesses(String cpr);

    Leader findByInternalUuid(String internalUuid);

    @Query("SELECT l FROM leader l LEFT JOIN FETCH l.businesses WHERE l.internalUuid = :internalUuid")
    Leader findByInternalUuidWithBusinesses(String internalUuid);

    @Query("SELECT l FROM leader l LEFT JOIN FETCH l.businesses WHERE l.id = :id")
    Optional<Leader> findByIdWithBusinesses(long id);

    @Query("SELECT DISTINCT l FROM leader l LEFT JOIN FETCH l.businesses")
    List<Leader> findAllWithBusinesses();

    @Query("SELECT DISTINCT l FROM leader l JOIN l.businesses b WHERE b.id = :businessId")
    List<Leader> findByBusinessId(long businessId);
}
