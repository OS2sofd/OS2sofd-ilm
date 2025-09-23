package dk.digitalidentity.OS2ilm.model.datatable.dao;

import dk.digitalidentity.OS2ilm.model.view.EmployeeView;
import org.springframework.data.jpa.datatables.repository.DataTablesRepository;

public interface EmployeeDatatableDao extends DataTablesRepository<EmployeeView, Long> {

}
