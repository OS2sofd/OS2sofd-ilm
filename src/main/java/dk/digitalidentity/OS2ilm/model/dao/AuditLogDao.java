package dk.digitalidentity.OS2ilm.model.dao;

import dk.digitalidentity.OS2ilm.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogDao extends JpaRepository<AuditLog, Long> {
	AuditLog getById(Long id);
	List<AuditLog> findByCorrelationId(String id);
	List<AuditLog> findAllByTtsBetween(LocalDateTime from, LocalDateTime to);
	List<AuditLog> findByTtsBefore(LocalDateTime dateTime);

	@Query("SELECT a FROM auditlog a " +
			"LEFT JOIN FETCH a.details " +
			"WHERE a.id = :id")
	AuditLog findByIdWithDetails(@Param("id") long id);

	/* Need to do this, as there is no cascade-delete to auditlog_details table
	 *
	 * DELETE dd
	 * FROM auditlogs_details dd
	 * JOIN (
	 *   SELECT d.id
	 *   FROM auditlogs_details d
	 *   LEFT JOIN auditlogs a ON d.id = a.auditlogs_details_id
	 *   WHERE a.id IS NULL
	 *   LIMIT 25000
	 * ) ss ON ss.id = dd.id
	 */
	@Modifying
	@Query(nativeQuery = true, value = "DELETE dd FROM auditlog_detail dd JOIN (SELECT d.id FROM auditlog_detail d LEFT JOIN auditlogs a ON d.id = a.auditlogs_details_id WHERE a.id IS NULL LIMIT 25000) ss ON ss.id = dd.id")
	void deleteUnreferencedAuditlogDetails();

	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM auditlog WHERE tts < ?1 LIMIT 25000")
	void deleteByTtsBefore(LocalDateTime before);
}
