package dk.digitalidentity.OS2ilm.controller.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dk.digitalidentity.OS2ilm.model.datatable.dao.ArchetypeDatatableDao;
import dk.digitalidentity.OS2ilm.model.entity.AdGroup;
import dk.digitalidentity.OS2ilm.model.entity.Archetype;
import dk.digitalidentity.OS2ilm.model.entity.RoleGroup;
import dk.digitalidentity.OS2ilm.model.entity.UserRole;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.model.role.RequireReadOnlyAdministrator;
import dk.digitalidentity.OS2ilm.model.role.RequireSystemAdministrator;
import dk.digitalidentity.OS2ilm.model.view.ArchetypeView;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.AdGroupService;
import dk.digitalidentity.OS2ilm.service.ArchetypeService;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.RoleGroupService;
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
	private final RoleGroupService roleGroupService;

	record ArchetypeRecord(Long id, String name) {}
	@PostMapping("/delete/{id}")
	public ResponseEntity<?> deleteArchetype(@PathVariable long id) {
		Archetype archetype = archetypeService.findById(id);
		if (archetype == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		ArchetypeRecord archetypeRecord = new ArchetypeRecord(archetype.getId(), archetype.getName());
		archetypeService.delete(archetype);

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), id, archetypeRecord.name, LogAction.ARCHETYPE_DELETED, "Arketype blev slettet", archetypeRecord);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireReadOnlyAdministrator
	@PostMapping("/view")
	public DataTablesOutput<ArchetypeView> archetypeTable(@RequestBody DataTablesInput input) {
		return archetypeDatatableDao.findAll(input);
	}

	public record CreateArchetype(String name, List<Long> roles, List<Long> groups, List<Long> roleGroups) {}
	public record CreateArchetypeLogRecord(Long id, String name, List<String> roles, List<String> groups, List<String> roleGroups) {}
	@PostMapping("/create")
	public ResponseEntity<?> createArchetype(@RequestBody CreateArchetype createArchetype) {
		if (!StringUtils.hasText(createArchetype.name)) {
			log.warn("Archetype needs to have a name");
			return new ResponseEntity<>("Du skal angive et navn", HttpStatus.BAD_REQUEST);
		}

		boolean exists = archetypeService.findAll().stream()
				.anyMatch(existing -> Objects.equals(existing.getName(), createArchetype.name()));

		if (exists) {
			log.warn("Archetype already exists");
			return new ResponseEntity<>("Arketypen findes allerede", HttpStatus.BAD_REQUEST);
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

		if (createArchetype.roleGroups != null) {
			createArchetype.roleGroups().forEach(id -> {
				RoleGroup roleGroup = roleGroupService.findById(id);
				if (roleGroup != null) {
					archetype.addRoleGroup(roleGroup);
				}
			});
		}

		Archetype saved = archetypeService.save(archetype);

		List<String> roleNames = new ArrayList<>();
		saved.getUserRoles().forEach(userRole -> {
			roleNames.add("name: " + userRole.getName() + " with id: " + userRole.getId());
		});

		List<String> groupNames = new ArrayList<>();
		saved.getAdGroups().forEach(group -> {
			groupNames.add("name: " + group.getName() + " with id: " + group.getId());
		});

		List<String> roleGroupNames = new ArrayList<>();
		saved.getRoleGroups().forEach(roleGroup -> {
			roleGroupNames.add("name: " + roleGroup.getName() + " with id: " + roleGroup.getId());
		});

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), saved.getId(), saved.getName(), LogAction.ARCHETYPE_CREATED, "Arketype blev oprettet", new CreateArchetypeLogRecord(saved.getId(), saved.getName(), roleNames, groupNames, roleGroupNames));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	public record EditArchetype(long archetypeId, String name, List<Long> rolesToAdd, List<Long> groupsToAdd, List<Long> roleGroupsToAdd, List<Long> rolesToRemove, List<Long> groupsToRemove, List<Long> roleGroupsToRemove) {}
	public record EditArchetypeLogRecord(Long id, String name, List<String> addedRoles, List<String> addedGroups, List<String> addedRoleGroups, List<String> removedRoles, List<String> removedGroups, List<String> removedRoleGroups) {}
	@Transactional
	@PostMapping("/edit")
	public ResponseEntity<?> editArchetype(@RequestBody EditArchetype editArchetype) {
		Archetype archetype = archetypeService.findById(editArchetype.archetypeId);
		if (archetype == null) {
			log.warn("Kunne ikke finde arketypen med ID: {}",  editArchetype.archetypeId);
			return new ResponseEntity<>("Arketypen findes ikke", HttpStatus.BAD_REQUEST);
		}

		archetype.setName(editArchetype.name);

		List<String> removedRoles = new ArrayList<>();
		editArchetype.rolesToRemove().forEach(id -> {
			UserRole userRole = userRoleService.findById(id);
			if (userRole != null) {
				removedRoles.add("name: " + userRole.getName() + " with id: " + id);
				archetype.removeUserRole(userRole);
			}
		});

		List<String> removedGroups = new ArrayList<>();
		editArchetype.groupsToRemove().forEach(id -> {
			AdGroup adGroup = adGroupService.findById(id);
			if (adGroup != null) {
				removedGroups.add("name: " + adGroup.getName() + " with id: " + id);
				archetype.removeAdGroup(adGroup);
			}
		});

		List<String> removedRoleGroups = new ArrayList<>();
		editArchetype.roleGroupsToRemove().forEach(id -> {
			RoleGroup roleGroup = roleGroupService.findById(id);
			if (roleGroup != null) {
				removedRoleGroups.add("name: " + roleGroup.getName() + " with id: " + id);
				archetype.removeRoleGroup(roleGroup);
			}
		});

		List<String> addedRoles = new ArrayList<>();
		editArchetype.rolesToAdd().forEach(id -> {
			UserRole userRole = userRoleService.findById(id);
			if (userRole != null) {
				addedRoles.add("name: " + userRole.getName() + " with id: " + id);
				archetype.addUserRole(userRole);
			}
		});

		List<String> addedGroups = new ArrayList<>();
		editArchetype.groupsToAdd().forEach(id -> {
			AdGroup adGroup = adGroupService.findById(id);
			if (adGroup != null) {
				addedGroups.add("name: " + adGroup.getName() + " with id: " + id);
				archetype.addAdGroup(adGroup);
			}
		});

		List<String> addedRoleGroups = new ArrayList<>();
		editArchetype.roleGroupsToAdd().forEach(id -> {
			RoleGroup roleGroup = roleGroupService.findById(id);
			if (roleGroup != null) {
				addedRoleGroups.add("name: " + roleGroup.getName() + " with id: " + id);
				archetype.addRoleGroup(roleGroup);
			}
		});

		Archetype saved = archetypeService.save(archetype);

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), saved.getId(), saved.getName(), LogAction.ARCHETYPE_UPDATED, "Arketype blev opdateret", new EditArchetypeLogRecord(saved.getId(), saved.getName(), addedRoles, addedGroups, addedRoleGroups, removedRoles, removedGroups, removedRoleGroups));
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
