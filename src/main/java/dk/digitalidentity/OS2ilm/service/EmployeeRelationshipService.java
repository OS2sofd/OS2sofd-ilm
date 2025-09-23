package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.entity.AdGroup;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.UserRole;
import dk.digitalidentity.OS2ilm.model.exception.AdGroupNotFoundException;
import dk.digitalidentity.OS2ilm.model.exception.EmployeeNotFoundException;
import dk.digitalidentity.OS2ilm.model.exception.UserRoleNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class EmployeeRelationshipService {
	private final ActiveDirectorySyncService activeDirectorySyncService;
	private final AdGroupService adGroupService;
	private final UserRoleService userRoleService;
	private final RoleCatalogueService roleCatalogueService;
	private final EmployeeService employeeService;

	public void updateEmployeeGroups(long employeeId, List<Long> groupIds) {
		Employee persistentEmployee = employeeService.findById(employeeId);
		groupIds.forEach(group -> {
			AdGroup adGroup = adGroupService.findById(group);
			persistentEmployee.addAdGroup(adGroup);

			// Call active directory and save
			activeDirectorySyncService.setGroupMembership(persistentEmployee, adGroup);
		});
	}

	public void updateEmployeeRoles(long employeeId, List<Long> roleIds) {
		Employee persistentEmployee = employeeService.findById(employeeId);
		roleIds.forEach(role -> {
			UserRole userRole = userRoleService.findById(role);
			persistentEmployee.addUserRole(userRole);

			// Call rolecatalogue and save
			boolean response;
			if (StringUtils.hasText(persistentEmployee.getMitIdUuid())) {
				response = roleCatalogueService.assignUserRoleToUser(userRole.getUserRoleId(), persistentEmployee.getMitIdUuid());
			} else {
				response = roleCatalogueService.assignUserRoleToUser(userRole.getUserRoleId(), persistentEmployee.getUser().getName());
			}
			if (response) {
				log.debug("Assigned " + userRole.getUserRoleId() + " to " + (StringUtils.hasText(persistentEmployee.getMitIdUuid()) ? persistentEmployee.getMitIdUuid() : persistentEmployee.getUser().getName()));
			}
			else {
				log.debug("Failed to assign " + userRole.getUserRoleId() + " to " + (StringUtils.hasText(persistentEmployee.getMitIdUuid()) ? persistentEmployee.getMitIdUuid() : persistentEmployee.getUser().getName()));
			}
		});
	}

	public void deleteEmployeeGroups(long employeeId, long groupId) {
		Employee employee = employeeService.findById(employeeId);
		if (employee == null) {
			log.warn("Employee with id {} not found", employeeId);
			throw new EmployeeNotFoundException(employeeId);
		}

		AdGroup adGroup = adGroupService.findById(groupId);
		if (adGroup == null) {
			log.warn("Group with id {} not found", groupId);
			throw new AdGroupNotFoundException(groupId);
		}

		employee.getAdGroups().remove(adGroup);
		employeeService.save(employee);

		// Remove the group membership in AD
		activeDirectorySyncService.deleteGroupMembership(employee, adGroup.getId());
	}

	public void deleteEmployeeRoles(long employeeId, long roleId) {
		Employee employee = employeeService.findById(employeeId);
		if (employee == null) {
			log.warn("Employee with id {} not found", employeeId);
			throw new EmployeeNotFoundException(employeeId);
		}

		UserRole userRole = userRoleService.findById(roleId);
		if (userRole == null) {
			log.warn("Group with id {} not found", roleId);
			throw new UserRoleNotFoundException(roleId);
		}

		employee.getUserRoles().remove(userRole);
		employeeService.save(employee);

		// Call to role catalogue
		roleCatalogueService.unassignUserRoleToUser(userRole.getUserRoleId(), StringUtils.hasText(employee.getMitIdUuid()) ? employee.getMitIdUuid() : employee.getUser().getName());
	}

}
