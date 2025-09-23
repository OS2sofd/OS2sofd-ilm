package dk.digitalidentity.OS2ilm.model.datatable.dao;

import dk.digitalidentity.OS2ilm.model.view.ApprovedEmployeeView;
import org.springframework.data.jpa.datatables.repository.DataTablesRepository;

public interface ApprovedEmployeeDatatableDao extends DataTablesRepository<ApprovedEmployeeView, Long> {
}
