package dk.digitalidentity.OS2ilm.controller.rest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import dk.digitalidentity.OS2ilm.model.datatable.dao.ApprovedEmployeeDatatableDao;
import dk.digitalidentity.OS2ilm.model.entity.Archetype;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.OrgUnit;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmployeeStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.model.role.RequireEmployeeAdministrator;
import dk.digitalidentity.OS2ilm.model.role.RequireEmployeeAdministratorOrHigher;
import dk.digitalidentity.OS2ilm.model.view.ApprovedEmployeeView;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.ActiveDirectorySyncService;
import dk.digitalidentity.OS2ilm.service.ArchetypeService;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.OrgUnitService;
import dk.digitalidentity.OS2ilm.service.SofdService;
import dk.digitalidentity.OS2ilm.util.DatatableSpecBuilderUtil;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@Slf4j
@RequestMapping("/rest")
@RestController
@RequiredArgsConstructor
@RequireEmployeeAdministrator
public class ApprovalRestController {
	private final EmployeeService employeeService;
	private final SecurityUtil securityUtil;
	private final ActiveDirectorySyncService activeDirectorySyncService;
	private final ApprovedEmployeeDatatableDao approvedEmployeeDatatableDao;
	private final OrgUnitService orgUnitService;
	private final ArchetypeService archetypeService;
	private final BusinessService businessService;
	private final AuditLogService auditLogService;
	private final SofdService sofdService;

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	record EmployeeRecord(String name, String email) {}

	@PostMapping("/approve/{id}")
	public ResponseEntity<?> approveEmployee(@PathVariable long id) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			log.warn("Employee with id {} not found", id);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		employee.setApproved(ApproveStatus.LEADER_APPROVED);

		employeeService.save(employee);

		auditLogService.log(securityUtil.getMitIdUuid(), securityUtil.getName(), id, employee.getName(), LogAction.EMPLOYEE_LEADER_APPROVED, "Medarbejder er blevet godkendt af en leder", new EmployeeRecord(employee.getName(), employee.getEmail()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/deny/{id}")
	public ResponseEntity<?> denyEmployee(@PathVariable long id) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			log.warn("Employee with id {} not found", id);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		employee.setApproved(ApproveStatus.DENIED);
		employeeService.save(employee);

		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), id, employee.getName(), LogAction.EMPLOYEE_DENIED, "Medarbejder er blevet afvist", new EmployeeRecord(employee.getName(), employee.getEmail()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireEmployeeAdministratorOrHigher
	@PostMapping("/approve/view")
	public DataTablesOutput<ApprovedEmployeeView> approvedTable(@RequestBody DataTablesInput input) {
		List<String> cvrs = securityUtil.getCvrs();

		boolean isLeader = securityUtil.isLeader();
		boolean isAdmin = securityUtil.isAdmin();

		DataTablesOutput<ApprovedEmployeeView> result;

		if (cvrs == null || cvrs.isEmpty()) {
			if (isLeader && !isAdmin) {
				Specification<ApprovedEmployeeView> spec = DatatableSpecBuilderUtil.getLeaderSpecificationWithoutCVR();
				result = approvedEmployeeDatatableDao.findAll(input, spec);
			} else if (isAdmin) {
				Specification<ApprovedEmployeeView> spec = DatatableSpecBuilderUtil.getAdminSpecificationWithoutCVR();
				result = approvedEmployeeDatatableDao.findAll(input, spec);
			} else {
				result = approvedEmployeeDatatableDao.findAll(input);
			}
		} else {
			Specification<ApprovedEmployeeView> spec;

			if (isLeader && !isAdmin) {
				spec = DatatableSpecBuilderUtil.getLeaderSpecification(cvrs);
			} else if (isAdmin) {
				spec = DatatableSpecBuilderUtil.getAdminSpecification(cvrs);
			} else {
				spec = DatatableSpecBuilderUtil.getApprovedEmployeeViewSpecification(cvrs);
			}

			result = approvedEmployeeDatatableDao.findAll(input, spec);
		}

		return result;
	}

	public record ApproveEmployeeRecord(boolean approved, boolean denied, long archetype, String ou, String startDate, String stopDate, long group, long activationPeriod, String businessName) {}
	@PostMapping("/approval/{id}")
	public ResponseEntity<?> adminApproval(@RequestBody ApproveEmployeeRecord approveEmployeeRecord, @PathVariable Long id) {
		Employee employee = employeeService.findByIdWithUserRolesAndAdGroups(id);
		if (employee == null) {
			log.warn("The employee does not exist");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		if (approveEmployeeRecord.approved) {
			log.debug("Approved employee with id {}", employee.getId());
			employee.setApproved(ApproveStatus.ADMIN_APPROVED);
		}
		else {
			log.debug("Denied employee with id {}", employee.getId());
			employee.setApproved(ApproveStatus.DENIED);
			employeeService.save(employee);
			return new ResponseEntity<>(HttpStatus.OK);
		}

		String startDateString = approveEmployeeRecord.startDate.substring(0, 10);
		String stopDateString = approveEmployeeRecord.stopDate.substring(0, 10);

		LocalDate startDate = LocalDate.parse(startDateString, DATE_FORMATTER);
		LocalDate stopDate = LocalDate.parse(stopDateString, DATE_FORMATTER);

		employee.setStartDate(startDate);
		employee.setStopDate(stopDate);


		OrgUnit orgUnit = orgUnitService.findOrgUnitByUuid(approveEmployeeRecord.ou);
		if (orgUnit == null) {
			log.warn("The orgunit does not exist");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		employee.setOrgUnit(orgUnit);
		employee.setIntegrated(EmployeeStatus.AWAITING_FULL_INTEGRATION);

		Archetype archetype = archetypeService.findByIdWithUserRolesAndAdGroups(approveEmployeeRecord.archetype);
		if (archetype == null) {
			log.warn("The archetype does not exist");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		if (archetype.getAdGroups() != null) {
			archetype.getAdGroups().forEach(employee::addAdGroup);
		}
		if (archetype.getUserRoles() != null) {
			archetype.getUserRoles().forEach(employee::addUserRole);
		}

		Employee savedEmployee = employeeService.save(employee);

		// Set a prefix if it doesn't exist yet (can happen when registering with a new business)
		Business business = savedEmployee.getBusiness();
		if (StringUtils.hasText(approveEmployeeRecord.businessName) && !StringUtils.hasText(business.getPrefix())) {
			business.setPrefix(approveEmployeeRecord.businessName);
			businessService.save(business);
		}

		// Make an AD account for the employee after being approved
		Employee updated = activeDirectorySyncService.createAdUser(savedEmployee, approveEmployeeRecord.activationPeriod);
		if (archetype.getAdGroups() != null) {
			archetype.getAdGroups().forEach(group -> activeDirectorySyncService.setGroupMembership(updated, group));
		}

		// Create person in Sofd
		if (StringUtils.hasText(savedEmployee.getCpr())) {
			sofdService.sofdCreateAndUpdateHelper(savedEmployee);
		}

		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), id, savedEmployee.getName(), LogAction.EMPLOYEE_ADMIN_APPROVED, "Medarbejder er blevet godkendt af en administrator", new EmployeeRecord(savedEmployee.getName(), savedEmployee.getEmail()));

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
