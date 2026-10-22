package dk.digitalidentity.OS2ilm.service;

import java.util.ArrayList;
import java.util.List;

import dk.digitalidentity.OS2ilm.model.exception.ActiveDirectoryIntegrationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dk.digitalidentity.OS2ilm.model.entity.AdGroup;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.RoleGroup;
import dk.digitalidentity.OS2ilm.model.entity.UserRole;
import dk.digitalidentity.OS2ilm.model.exception.AdGroupNotFoundException;
import dk.digitalidentity.OS2ilm.model.exception.EmployeeNotFoundException;
import dk.digitalidentity.OS2ilm.model.exception.RoleCatalogIntegrationException;
import dk.digitalidentity.OS2ilm.model.exception.RoleGroupNotFoundException;
import dk.digitalidentity.OS2ilm.model.exception.UserRoleNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class EmployeeRelationshipService {
	private final AdGroupService adGroupService;
	private final UserRoleService userRoleService;
	private final RoleCatalogueService roleCatalogueService;
	private final EmployeeService employeeService;
	private final RoleGroupService roleGroupService;
	private final ADQueueService aDQueueService;

	public List<String> updateEmployeeGroups(long employeeId, List<Long> groupIds) {
		Employee persistentEmployee = employeeService.findById(employeeId);
		if (persistentEmployee == null) {
			throw new EmployeeNotFoundException(employeeId);
		}

		List<String> addedGroups = new ArrayList<>();
		List<AdGroup> adGroups = groupIds.stream()
				.map(groupId -> {
					AdGroup adGroup = adGroupService.findById(groupId);
					if (adGroup == null) {
						throw new AdGroupNotFoundException(groupId);
					}

					addedGroups.add("name: " + adGroup.getName() + " with id: " + groupId);
					return adGroup;
				})
				.toList();

		adGroups.forEach(adGroup -> {
			// Call active directory
			aDQueueService.addSetGroupMembershipToQueue(persistentEmployee.getId(), adGroup.getId());

			// Optimistic update - will be rolled back by queue handler if AD operation fails after max retries
			persistentEmployee.addAdGroup(adGroup);
		});

		employeeService.save(persistentEmployee);
		log.debug("Successfully updated employee groups");
		return addedGroups;
	}

	public List<String> updateEmployeeRoles(long employeeId, List<Long> roleIds) {
		Employee persistentEmployee = employeeService.findById(employeeId);
		if (persistentEmployee == null) {
			throw new EmployeeNotFoundException(employeeId);
		}

		List<String> addedRoles = new ArrayList<>();
		List<UserRole> userRoles = roleIds.stream()
				.map(roleId -> {
					UserRole userRole = userRoleService.findById(roleId);
					if (userRole == null) {
						throw new UserRoleNotFoundException(roleId);
					}

					addedRoles.add("name: " + userRole.getName() + " with id: " + roleId);
					return userRole;
				})
				.toList();

		String identifier = configureIdentifier(employeeId, persistentEmployee);

		userRoles.forEach(userRole -> {
			// Call role catalogue
			boolean response = roleCatalogueService.assignUserRoleToUser(userRole.getUserRoleId(), identifier);

			if (!response) {
				throw new RoleCatalogIntegrationException(
						"Failed to assign role " + userRole.getId() + " for employee " + employeeId + " (identifier: " + identifier + ") in role catalogue"
				);
			}

			// Set locally after role catalogue call succeeded
			persistentEmployee.addUserRole(userRole);
		});

		employeeService.save(persistentEmployee);
		log.debug("Successfully updated employee roles");
		return addedRoles;
	}

	public List<String> updateEmployeeRoleGroups(long employeeId, List<Long> roleGroupIds) {
		Employee persistentEmployee = employeeService.findById(employeeId);
		if (persistentEmployee == null) {
			throw new EmployeeNotFoundException(employeeId);
		}

		List<String> addedRoleGroups = new ArrayList<>();
		List<RoleGroup> roleGroups = roleGroupIds.stream()
				.map(roleGroupId -> {
					RoleGroup roleGroup = roleGroupService.findById(roleGroupId);
					if (roleGroup == null) {
						throw new RoleGroupNotFoundException(roleGroupId);
					}

					addedRoleGroups.add("name: " + roleGroup.getName() + " with id: " + roleGroupId);
					return roleGroup;
				})
				.toList();

		String identifier = configureIdentifier(employeeId, persistentEmployee);

		roleGroups.forEach(roleGroup -> {
			// Call role catalogue
			boolean response = roleCatalogueService.assignRoleGroupToUser(roleGroup.getRoleGroupId(), identifier);

			if (!response) {
				throw new RoleCatalogIntegrationException(
						"Failed to assign role group " + roleGroup.getId() + " for employee " + employeeId + " (identifier: " + identifier + ") in role catalogue"
				);
			}

			// Set locally after the call to role catalogue succeeded
			persistentEmployee.addRoleGroup(roleGroup);
		});

		employeeService.save(persistentEmployee);
		log.debug("Successfully updated employee roleGroups");
		return addedRoleGroups;
	}

	public void deleteEmployeeGroups(long employeeId, long groupId) {
		Employee employee = employeeService.findById(employeeId);
		if (employee == null) {
			throw new EmployeeNotFoundException(employeeId);
		}

		AdGroup adGroup = adGroupService.findById(groupId);
		if (adGroup == null) {
			throw new AdGroupNotFoundException(groupId);
		}

		// Remove the group membership in AD
		aDQueueService.addDeleteGroupMembershipToQueue(employee.getId(), adGroup.getId());

		// Optimistic update - will be rolled back by queue handler if AD operation fails after max retries
		employee.getAdGroups().remove(adGroup);
		employeeService.save(employee);
		log.debug("Successfully deleted employee groups");
	}

	public void deleteEmployeeRole(long employeeId, long roleId) {
		Employee employee = employeeService.findById(employeeId);
		if (employee == null) {
			throw new EmployeeNotFoundException(employeeId);
		}

		UserRole userRole = userRoleService.findById(roleId);
		if (userRole == null) {
			throw new UserRoleNotFoundException(roleId);
		}

		String identifier = configureIdentifier(employeeId, employee);

		// Call to role catalogue
		boolean response = roleCatalogueService.unassignUserRoleToUser(userRole.getUserRoleId(), identifier);

		if (!response) {
			throw new RoleCatalogIntegrationException(
					"Failed to unassign role " + roleId + " from employee " + employeeId + " (identifier: " + identifier + ") in role catalogue"
			);
		}

		// Modify local state after call succeeds
		employee.getUserRoles().remove(userRole);
		employeeService.save(employee);
		log.debug("Successfully deleted employee roles");
	}

	public void deleteEmployeeRoleGroup(long employeeId, long roleGroupId) {
		Employee employee = employeeService.findById(employeeId);
		if (employee == null) {
			throw new EmployeeNotFoundException(employeeId);
		}

		RoleGroup roleGroup = roleGroupService.findById(roleGroupId);
		if (roleGroup == null) {
			throw new RoleGroupNotFoundException(roleGroupId);
		}

		String identifier = configureIdentifier(employeeId, employee);

		// Call to role catalogue
		boolean response = roleCatalogueService.unassignRoleGroupToUser(roleGroup.getRoleGroupId(), identifier);

		if (!response) {
			throw new RoleCatalogIntegrationException(
					"Failed to unassign role group " + roleGroupId + " from employee " + employeeId + " (identifier: " + identifier + ") in role catalogue"
			);
		}

		employee.getRoleGroups().remove(roleGroup);
		employeeService.save(employee);
		log.debug("Successfully deleted employee roleGroups");
	}

	private String configureIdentifier(long employeeId, Employee employee) {
		String identifier;
		if (StringUtils.hasText(employee.getMitIdUuid())) {
			identifier = employee.getMitIdUuid();
		} else if (employee.getUser() != null && StringUtils.hasText(employee.getUser().getName())) {
			identifier = employee.getUser().getName();
		} else {
			throw new RoleCatalogIntegrationException(
					"Employee " + employeeId + " has no valid identifier (mitIdUUid or ad-username) for role catalogue integration"
			);
		}

		return identifier;
	}

	public List<String> removeRoles(long employeeId, List<Long> roleIds) {
		List<String> removed = new ArrayList<>();
		for (Long rId : roleIds) {
			UserRole userRole = userRoleService.findById(rId);
			if (userRole == null) {
				log.warn("Attempted to remove non-existent role ID {} from employee {}", rId, employeeId);
				continue;
			}
			try {
				removed.add("name: " + userRole.getName() + " with id: " + rId);
				deleteEmployeeRole(employeeId, userRole.getId());
			}
			catch (Exception ex) {
				throw new RoleCatalogIntegrationException(ex.getMessage(), ex);
			}
		}
		return removed;
	}

	public List<String> removeGroups(long employeeId, List<Long> groupIds) {
		List<String> removed = new ArrayList<>();
		for (Long gId : groupIds) {
			AdGroup adGroup = adGroupService.findById(gId);
			if (adGroup == null) {
				log.warn("Attempted to remove non-existent group ID {} from employee {}", gId, employeeId);
				continue;
			}
			try {
				removed.add("name: " + adGroup.getName() + " with id: " + gId);
				deleteEmployeeGroups(employeeId, adGroup.getId());
			}
			catch (Exception ex) {
				throw new ActiveDirectoryIntegrationException(ex.getMessage(), ex);
			}
		}
		return removed;
	}

	public List<String> removeRoleGroups(long employeeId, List<Long> roleGroupIds) {
		List<String> removed = new ArrayList<>();
		for (Long rgId : roleGroupIds) {
			RoleGroup roleGroup = roleGroupService.findById(rgId);
			if (roleGroup == null) {
				log.warn("Attempted to remove non-existent role group ID {} from employee {}", rgId, employeeId);
				continue;
			}
			try {
				removed.add("name: " + roleGroup.getName() + " with id: " + rgId);
				deleteEmployeeRoleGroup(employeeId, roleGroup.getId());
			}
			catch (Exception ex) {
				throw new RoleCatalogIntegrationException(ex.getMessage(), ex);
			}
		}
		return removed;
	}
}
