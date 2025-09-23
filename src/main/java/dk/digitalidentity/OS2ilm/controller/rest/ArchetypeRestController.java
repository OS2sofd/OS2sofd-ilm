package dk.digitalidentity.OS2ilm.controller.rest;

import java.util.List;
import java.util.Objects;

import dk.digitalidentity.OS2ilm.model.datatable.dao.ArchetypeDatatableDao;
import dk.digitalidentity.OS2ilm.model.entity.AdGroup;
import dk.digitalidentity.OS2ilm.model.entity.Archetype;
import dk.digitalidentity.OS2ilm.model.entity.UserRole;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.model.role.RequireReadOnlyAdministrator;
import dk.digitalidentity.OS2ilm.model.role.RequireSystemAdministrator;
import dk.digitalidentity.OS2ilm.model.view.ArchetypeView;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.AdGroupService;
import dk.digitalidentity.OS2ilm.service.ArchetypeService;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.UserRoleService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Hidden
@RequestMapping("/rest/archetype")
@RestController
@RequireSystemAdministrator
@RequiredArgsConstructor
public class ArchetypeRestController {
	private final ArchetypeService archetypeService;
	private final ArchetypeDatatableDao archetypeDatatableDao;
	private final UserRoleService userRoleService;
	private final AdGroupService adGroupService;
	private final AuditLogService auditLogService;
	private final SecurityUtil securityUtil;

	record ArchetypeRecord(String name) {}

	@PostMapping("/delete/{id}")
	public ResponseEntity<?> deleteArchetype(@PathVariable long id) {
		Archetype archetype = archetypeService.findById(id);
		if (archetype == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		ArchetypeRecord archetypeRecord = new ArchetypeRecord(archetype.getName());

		archetypeService.delete(archetype);
		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), id, archetypeRecord.name, LogAction.ARCHETYPE_DELETED, "Arketype blev slettet", archetypeRecord);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireReadOnlyAdministrator
	@PostMapping("/view")
	public DataTablesOutput<ArchetypeView> archetypeTable(@RequestBody DataTablesInput input) {
		return archetypeDatatableDao.findAll(input);
	}

	public record CreateArchetype(String name, List<Long> roles, List<Long> groups) {}
	@PostMapping("/create")
	public ResponseEntity<?> createArchetype(@RequestBody CreateArchetype createArchetype) {
		if (!StringUtils.hasText(createArchetype.name)) {
			log.warn("Archetype needs to have a name");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		boolean exists = archetypeService.findAll().stream()
				.anyMatch(existing -> Objects.equals(existing.getName(), createArchetype.name()));

		if (exists) {
			log.warn("Archetype already exists");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		Archetype archetype = new Archetype();
		archetype.setName(createArchetype.name);

		if (createArchetype.roles != null) {
			createArchetype.roles().forEach(id -> {
				UserRole userRole = userRoleService.findById(id);
				if (userRole != null) {
					archetype.addUserRole(userRole);
				}
			});
		}

		if (createArchetype.groups != null) {
			createArchetype.groups().forEach(id -> {
				AdGroup adGroup = adGroupService.findById(id);
				if (adGroup != null) {
					archetype.addAdGroup(adGroup);
				}
			});
		}

		archetypeService.save(archetype);

		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), archetype.getId(), createArchetype.name, LogAction.ARCHETYPE_CREATED, "Arketype blev oprettet", new ArchetypeRecord(createArchetype.name));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	public record EditArchetype(long archetypeId, String name, List<Long> rolesToAdd, List<Long> groupsToAdd, List<Long> rolesToRemove, List<Long> groupsToRemove) {}
	@Transactional
	@PostMapping("/edit")
	public ResponseEntity<?> editArchetype(@RequestBody EditArchetype editArchetype) {
		Archetype archetype = archetypeService.findById(editArchetype.archetypeId);
		if (archetype == null) {
			log.warn("Archetype can't be found");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		archetype.setName(editArchetype.name);

		editArchetype.rolesToRemove().forEach(id -> {
			UserRole userRole = userRoleService.findById(id);
			if (userRole != null) {
				archetype.removeUserRole(userRole);
			}
		});

		editArchetype.groupsToRemove().forEach(id -> {
			AdGroup adGroup = adGroupService.findById(id);
			if (adGroup != null) {
				archetype.removeAdGroup(adGroup);
			}
		});

		editArchetype.rolesToAdd().forEach(id -> {
			UserRole userRole = userRoleService.findById(id);
			if (userRole != null) {
				archetype.addUserRole(userRole);
			}
		});

		editArchetype.groupsToAdd().forEach(id -> {
			AdGroup adGroup = adGroupService.findById(id);
			if (adGroup != null) {
				archetype.addAdGroup(adGroup);
			}
		});

		archetypeService.save(archetype);

		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), editArchetype.archetypeId, editArchetype.name, LogAction.ARCHETYPE_UPDATED, "Arketype blev opdateret", new ArchetypeRecord(editArchetype.name));
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
