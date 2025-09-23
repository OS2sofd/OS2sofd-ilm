package dk.digitalidentity.OS2ilm.model.datatable.dao;

import dk.digitalidentity.OS2ilm.model.view.AuditLogView;
import org.springframework.data.jpa.datatables.repository.DataTablesRepository;

public interface AuditLogDatatableDao extends DataTablesRepository<AuditLogView, Long> {

}
