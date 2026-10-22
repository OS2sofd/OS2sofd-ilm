package dk.digitalidentity.OS2ilm.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.ItSystem;
import dk.digitalidentity.OS2ilm.model.entity.RoleGroup;
import dk.digitalidentity.OS2ilm.model.entity.UserRole;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmployeeStatus;
import dk.digitalidentity.OS2ilm.service.RoleCatalogueStub.ItSystemDTO;
import dk.digitalidentity.OS2ilm.service.RoleCatalogueStub.RoleGroupDTO;
import dk.digitalidentity.OS2ilm.service.RoleCatalogueStub.UserRoleDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleCatalogueService {
	private final ItSystemService itSystemService;
	private final EmployeeService employeeService;
	private final RoleGroupService roleGroupService;
	private final RoleCatalogueStub roleCatalogueStub;

	public boolean assignUserRoleToUser(long userRoleId, String userUuid) {
		return roleCatalogueStub.assignUserRoleToUser(userRoleId, userUuid);
	}

	public boolean unassignUserRoleToUser(long userRoleId, String userUuid) {
		return roleCatalogueStub.removeUserRoleFromUser(userRoleId, userUuid);
	}

	public boolean assignRoleGroupToUser(long roleGroupId, String userUuid) {
		return roleCatalogueStub.assignRoleGroupToUser(roleGroupId, userUuid);
	}

	public boolean unassignRoleGroupToUser(long roleGroupId, String userUuid) {
		return roleCatalogueStub.removeRoleGroupFromUser(roleGroupId, userUuid);
	}

	@Transactional
	public void assignRolesToUsersInRc() {
		List<Employee> employees = employeeService.findByIntegratedAndLoaded(EmployeeStatus.AWAITING_RC_INTEGRATION);

		for (Employee employee : employees) {
			if (shouldSkipEmployee(employee)) {
				// If an employee has not been assigned any roles, they should not be synced in RC but they are still marked as integrated (Having no roles at this point must be intended)
				markEmployeeAsIntegrated(employee);
				continue;
			}

			boolean success = processEmployeeRoleAssignments(employee);

			if (success) {
				markEmployeeAsIntegrated(employee);
			}
		}
	}

	@Transactional(rollbackOn = Exception.class)
	public void syncUserRoles() {
		int updated = 0, created = 0, deleted = 0;

		try {
			List<ItSystemDTO> itSystemsFromRC = roleCatalogueStub.getItSystems();

			if (itSystemsFromRC != null) {
				List<ItSystem> itSystems = itSystemService.findAll();

				for (ItSystemDTO itSystemFromRC : itSystemsFromRC) {
					long itSystemId = itSystemFromRC.itSystemId();
					String itSystemName = itSystemFromRC.itSystemName();
					List<UserRoleDTO> roles = itSystemFromRC.roles();

					Optional<ItSystem> existingItSystem = itSystems.stream().filter(its -> its.getItSystemId() == itSystemId).findAny();
					ItSystem itSystem = null;
					if (existingItSystem.isPresent()) {
						//update
						itSystem = existingItSystem.get();
						boolean hasChanges = false;
						boolean userRoleChanges = updateRoles(itSystem, roles);

						if (!Objects.equals(itSystem.getName(), itSystemName)) {
							hasChanges = true;
						}

						if (userRoleChanges || hasChanges) {
							log.info("Updating ItSystem: {}", itSystem.getName());
							itSystem.setName(itSystemName);
							updated++;
						}
					}
					else {
						//create
						log.info("Creating ItSystem: " + itSystemName);
						itSystem = new ItSystem();
						itSystem.setItSystemId(itSystemId);
						itSystem.setName(itSystemName);
						itSystem.setUserRoles(new ArrayList<>());

						itSystem = itSystemService.save(itSystem);
						updateRoles(itSystem, roles);
						created++;
					}

					itSystemService.save(itSystem);
				}

				List<ItSystem> toBeDeleted = new ArrayList<>();
				for (ItSystem itSystem : itSystems) {
					Optional<ItSystemDTO> incomingItSystem = itSystemsFromRC.stream().filter(dto -> dto.itSystemId() == itSystem.getItSystemId()).findAny();
					if (incomingItSystem.isEmpty()) {
						toBeDeleted.add(itSystem);
						log.info("Deleting ItSystem: {}", itSystem.getName());
						deleted++;
					}
				}

				itSystemService.deleteAll(toBeDeleted);

				if (updated > 0 || created > 0 || deleted > 0) {
					log.debug("Sync Results Updated: {} Created: {} Deleted: {}", updated, created, deleted);
				}
			}
			else {
				log.error("Body received from role-catalogue was null.");
			}
		}
		catch (Exception ex) {
			log.error("Error occured while synchronizing data from role-catalogue.", ex);
		}
	}

	@Transactional(rollbackOn = Exception.class)
	public void syncRoleGroups() {
		int updated = 0, created = 0, deleted = 0;

		List<RoleGroupDTO> roleGroupDTOS = roleCatalogueStub.getAllRoleGroups();
		List<RoleGroup> roleGroups = roleGroupService.findAll();

		try {
			if (roleGroupDTOS != null) {
				for (RoleGroupDTO roleGroupDTO : roleGroupDTOS) {
					long roleGroupId = roleGroupDTO.id();
					String roleGroupName = roleGroupDTO.name();
					String roleGroupDescription = roleGroupDTO.description();

					RoleGroup existingRoleGroup = roleGroups.stream()
							.filter(roleGroup -> roleGroup.getRoleGroupId() == roleGroupId)
							.findAny()
							.orElse(null);

					RoleGroup roleGroup = null;
					if (existingRoleGroup != null) {
						roleGroup = existingRoleGroup;

						boolean hasChanges = false;

						if (!Objects.equals(existingRoleGroup.getName(), roleGroupName)) {
							hasChanges = true;
						}
						
						if (!Objects.equals(existingRoleGroup.getDescription(), roleGroupDescription)) {
							hasChanges = true;
						}

						if (hasChanges) {
							log.info("Updating RoleGroup: " + existingRoleGroup.getName());
							roleGroup.setName(roleGroupName);
							roleGroup.setDescription(roleGroupDescription);
							updated++;
						}
					}
					else {
						log.info("Creating RoleGroup: {}", roleGroupName);
						roleGroup = new RoleGroup();
						roleGroup.setRoleGroupId(roleGroupId);
						roleGroup.setName(roleGroupName);
						roleGroup.setDescription(roleGroupDescription);
						roleGroup = roleGroupService.save(roleGroup);
						created++;
					}

					roleGroupService.save(roleGroup);
				}

				List<RoleGroup> toBeDeleted = new ArrayList<>();
				for (RoleGroup roleGroup : roleGroups) {
					Optional<RoleGroupDTO> incomingRoleGroup = roleGroupDTOS.stream()
							.filter(dto -> dto.id() == roleGroup.getRoleGroupId())
							.findAny();

					if (incomingRoleGroup.isEmpty()) {
						toBeDeleted.add(roleGroup);
						log.info("Deleting RoleGroup: {}", roleGroup.getName());
						deleted++;
					}
				}

				roleGroupService.deleteAll(toBeDeleted);

				if (updated > 0 || created > 0 || deleted > 0) {
					log.debug("RoleGroup Sync Results - Updated: {} Created: {} Deleted: {}", updated, created, deleted);
				}
			}
			else {
				log.error("Body received from role-catalogue was null.");
			}
		}
		catch (Exception ex) {
			log.error("Error occured while synchronizing RoleGroups from role-catalogue.", ex);
		}
	}

	private boolean updateRoles(ItSystem itSystem, List<UserRoleDTO> roles) {
		int updated = 0, created = 0, deleted = 0;

		for (UserRoleDTO userRoleDTO : roles) {
			Optional<UserRole> userRole = itSystem.getUserRoles().stream().filter(ur -> ur.getUserRoleId() == userRoleDTO.id()).findAny();
			if (userRole.isPresent()) {
				//Update
				UserRole existingUserRole = userRole.get();
				boolean hasChanges = false;

				if (!Objects.equals(existingUserRole.getName(), userRoleDTO.name())) {
					hasChanges = true;
				}
				if (!Objects.equals(existingUserRole.getDescription(), userRoleDTO.description())) {
					hasChanges = true;
				}

				if (hasChanges) {
					log.debug("\tUpdating UserRole: {}", userRole.get().getName());

					existingUserRole.setName(userRoleDTO.name());
					existingUserRole.setDescription(userRoleDTO.description());

					updated++;
				}
			}
			else {
				//Create
				log.debug("\tCreating UserRole: {}", userRoleDTO.name());
				UserRole newUserRole = new UserRole();
				newUserRole.setUserRoleId(userRoleDTO.id());
				newUserRole.setName(userRoleDTO.name());
				newUserRole.setDescription(userRoleDTO.description());
				newUserRole.setItSystem(itSystem);

				itSystem.getUserRoles().add(newUserRole);
				created++;
			}
		}

		//Delete UserRoles
		for (Iterator<UserRole> iterator = itSystem.getUserRoles().iterator(); iterator.hasNext();) {
			UserRole userRole = iterator.next();
			if (roles.stream().noneMatch(dto -> dto.id() == userRole.getUserRoleId())) {
				log.debug("Deleting UserRole: {}", userRole.getName());
				iterator.remove();
				deleted++;
			}
		}

		log.debug("Updated: {} Created: {} Deleted: {}", updated, created, deleted);

		return (updated != 0 || created != 0 || deleted != 0);
	}

	private boolean shouldSkipEmployee(Employee employee) {
		boolean hasNoUserRoles = employee.getUserRoles() == null || employee.getUserRoles().isEmpty();
		boolean hasNoRoleGroups = employee.getRoleGroups() == null || employee.getRoleGroups().isEmpty();

		return hasNoUserRoles && hasNoRoleGroups;
	}

	private boolean processEmployeeRoleAssignments(Employee employee) {
		boolean successUserRoles = assignUserRolesToEmployee(employee);
		boolean successRoleGroups = assignRoleGroupsToEmployee(employee);

		return successUserRoles && successRoleGroups;
	}

	private boolean assignUserRolesToEmployee(Employee employee) {
		if (employee.getUserRoles() == null || employee.getUserRoles().isEmpty()) {
			return true;
		}

		boolean failedFullSync = false;
		for (UserRole role : employee.getUserRoles()) {
			boolean success = assignSingleUserRole(employee, role);

			if (!success) {
				failedFullSync = true;
			}
		}

		return !failedFullSync;
	}

	private boolean assignRoleGroupsToEmployee(Employee employee) {
		if (employee.getRoleGroups() == null || employee.getRoleGroups().isEmpty()) {
			return true;
		}

		boolean failedFullSync = false;
		for (RoleGroup roleGroup : employee.getRoleGroups()) {
			boolean success = assignSingleRoleGroup(employee, roleGroup);

			if (!success) {
				failedFullSync = true;
			}
		}

		return !failedFullSync;
	}

	private boolean assignSingleUserRole(Employee employee, UserRole role) {
		String userIdentifier = getUserIdentifier(employee);
		if (userIdentifier == null) {
			log.warn("Failed to assign {} to {} because the employee has no uuid and aduser",
					role.getUserRoleId(), employee.getName());
			return false;
		}

		boolean success = assignUserRoleToUser(role.getUserRoleId(), userIdentifier);
		logRoleAssignmentResult(success, role.getUserRoleId(), userIdentifier);

		return success;
	}

	private boolean assignSingleRoleGroup(Employee employee, RoleGroup roleGroup) {
		String userIdentifier = getUserIdentifier(employee);
		if (userIdentifier == null) {
			log.warn("Failed to assign {} to {} because the employee has no uuid and aduser",
					roleGroup.getRoleGroupId(), employee.getName());
			return false;
		}

		boolean success = assignRoleGroupToUser(roleGroup.getRoleGroupId(), userIdentifier);
		logRoleAssignmentResult(success, roleGroup.getRoleGroupId(), userIdentifier);

		return success;
	}

	private String getUserIdentifier(Employee employee) {
		if (StringUtils.hasText(employee.getMitIdUuid())) {
			return employee.getMitIdUuid();
		}

		if (employee.getUser() != null) {
			return employee.getUser().getName();
		}

		return null;
	}

	private void logRoleAssignmentResult(boolean success, long roleId, String userIdentifier) {
		if (success) {
			log.debug("Assigned {} to {}", roleId, userIdentifier);
		} else {
			log.debug("Failed to assign {} to {}", roleId, userIdentifier);
		}
	}

	private void markEmployeeAsIntegrated(Employee employee) {
		employee.setIntegrated(EmployeeStatus.FULLY_INTEGRATED);
		employeeService.save(employee);
	}
}
