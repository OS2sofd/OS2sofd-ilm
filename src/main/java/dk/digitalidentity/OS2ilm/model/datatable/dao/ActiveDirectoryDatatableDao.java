package dk.digitalidentity.OS2ilm.model.datatable.dao;

import dk.digitalidentity.OS2ilm.model.view.AdUserView;
import org.springframework.data.jpa.datatables.repository.DataTablesRepository;

public interface ActiveDirectoryDatatableDao extends DataTablesRepository<AdUserView, Long> {

}
