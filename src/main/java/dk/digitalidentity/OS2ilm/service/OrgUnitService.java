package dk.digitalidentity.OS2ilm.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import dk.digitalidentity.OS2ilm.model.dao.OrgUnitDao;
import dk.digitalidentity.OS2ilm.model.entity.OUTreeForm;
import dk.digitalidentity.OS2ilm.model.entity.OrgUnit;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrgUnitService {

	private static final String SELECT_THIN_ORGUNITS_SQL = "SELECT o.uuid, o.name AS name, o.parent_uuid FROM orgunit o;";

	private final OrgUnitDao orgUnitDao;
	private final JdbcTemplate jdbcTemplate;

	public List<OrgUnit> findAll() {
		return orgUnitDao.findAll();
	}

	public OrgUnit findOrgUnitByUuid(String uuid) {
		return orgUnitDao.findOrgUnitByUuid(uuid);
	}

	public OrgUnit save(OrgUnit orgUnit) {
		return orgUnitDao.save(orgUnit);
	}

	public void delete(OrgUnit orgUnit) {
		orgUnitDao.delete(orgUnit);
	}

	public List<String> findAllUuids() {
		List<OrgUnit> orgUnits = findAll();

		return orgUnits.stream().map(OrgUnit::getUuid).toList();
	}

	public List<OUTreeForm> getAllTree() {
		List<OUTreeForm> result = jdbcTemplate.query(SELECT_THIN_ORGUNITS_SQL, (RowMapper<OUTreeForm>) (rs, rownum) -> {
			OUTreeForm ou = new OUTreeForm();

			ou.setId(rs.getString("uuid"));
			ou.setText(rs.getString("name"));

			String parentUuid = rs.getString("parent_uuid");
			if (parentUuid != null && !parentUuid.isEmpty()) {
				ou.setParent(parentUuid);
			}
			else {
				ou.setParent("#");
			}

			return ou;
		});

		result.sort(Comparator.comparing(OUTreeForm::getText));

		return result;
	}

	public void deleteByUuidIn(List<String> uuids) {
		orgUnitDao.deleteByUuidIn(uuids);
	}
}
