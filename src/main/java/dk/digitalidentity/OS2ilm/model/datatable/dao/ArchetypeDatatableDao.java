package dk.digitalidentity.OS2ilm.model.datatable.dao;

import dk.digitalidentity.OS2ilm.model.view.ArchetypeView;
import org.springframework.data.jpa.datatables.repository.DataTablesRepository;

public interface ArchetypeDatatableDao extends DataTablesRepository<ArchetypeView, Long> {

}
