package dk.digitalidentity.OS2ilm.controller.rest;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.datatable.dao.EmployeeDatatableDao;
import dk.digitalidentity.OS2ilm.model.entity.ActivationTimeFrame;
import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.model.entity.Archetype;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.OrgUnit;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmployeeStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.model.role.RequireEmployeeAdministrator;
import dk.digitalidentity.OS2ilm.model.role.RequireEmployeeAdministratorOrHigher;
import dk.digitalidentity.OS2ilm.model.view.EmployeeView;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.ActivationTimeFrameService;
import dk.digitalidentity.OS2ilm.service.ActiveDirectorySyncService;
import dk.digitalidentity.OS2ilm.service.AdUserService;
import dk.digitalidentity.OS2ilm.service.ArchetypeService;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import dk.digitalidentity.OS2ilm.service.EmployeeRelationshipService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.MessageService;
import dk.digitalidentity.OS2ilm.service.OrgUnitService;
import dk.digitalidentity.OS2ilm.service.RoleCatalogueService;
import dk.digitalidentity.OS2ilm.service.SofdService;
import dk.digitalidentity.OS2ilm.service.cms.CmsMessageBundle;
import dk.digitalidentity.OS2ilm.util.DatatableSpecBuilderUtil;
import dk.digitalidentity.OS2ilm.util.RandomPasswordGeneratorUtil;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Hidden
@Slf4j
@RequestMapping("/rest/employee")
@RestController
@RequireEmployeeAdministrator
@RequiredArgsConstructor
public class EmployeeRestController {
	private final EmployeeService employeeService;
	private final BusinessService businessService;
	private final EmployeeDatatableDao employeeDatatableDao;
	private final ActiveDirectorySyncService activeDirectorySyncService;
	private final OrgUnitService orgUnitService;
	private final MessageService messageService;
	private final CmsMessageBundle cmsMessageBundle;
	private final SecurityUtil securityUtil;
	private final IlmConfiguration ilmConfiguration;
	private final ArchetypeService archetypeService;
	private final RoleCatalogueService roleCatalogueService;
	private final ActivationTimeFrameService activationTimeFrameService;
	private final EmployeeRelationshipService employeeRelationshipService;
	private final AuditLogService auditLogService;
	private final AdUserService adUserService;
	private final SofdService sofdService;

	record EmployeeLogRecord(String name, String email) {}

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public record EditEmployeeRecord(String name, String email, String phone, long businessId, String startDate, String stopDate, String position, List<Long> roles, List<Long> groups) {}
	@PatchMapping("/edit/{id}")
	public ResponseEntity<?> editEmployee(@PathVariable long id, @RequestBody EditEmployeeRecord editEmployeeRecord) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			log.warn("Employee with id {} not found", id);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		Business business = businessService.findById(editEmployeeRecord.businessId);
		if (business == null) {
			log.warn("Agency with id {} not found", editEmployeeRecord.businessId);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		LocalDate startDate = LocalDate.parse(editEmployeeRecord.startDate, DATE_FORMATTER);
		LocalDate stopDate = LocalDate.parse(editEmployeeRecord.stopDate, DATE_FORMATTER);

		employee.setName(editEmployeeRecord.name);
		employee.setEmail(editEmployeeRecord.email);
		employee.setPhone(editEmployeeRecord.phone);
		employee.setStartDate(startDate);
		employee.setStopDate(stopDate);
		employee.setBusiness(business);
		employee.setPositionName(editEmployeeRecord.position);

		// Take care of the relation, i.e., add to each entity collection for persistence
		employeeRelationshipService.updateEmployeeGroups(employee.getId(), editEmployeeRecord.groups);
		employeeRelationshipService.updateEmployeeRoles(employee.getId(), editEmployeeRecord.roles);

		Employee save = employeeService.save(employee);
		AdUser adUser = adUserService.findByEmployee(save);
//		adUser.setAdGroups(save.getAdGroups());
		adUserService.save(adUser);
		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), employee.getId(), save.getName(), LogAction.EMPLOYEE_UPDATED, "Medarbejder blev opdateret", new EmployeeLogRecord(save.getName(), save.getEmail()));

		// Call sofd and make changes there too
		if (StringUtils.hasText(employee.getCpr())) {
			sofdService.sofdCreateAndUpdateHelper(employee);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	public record CreateEmployeeRecord(String name, String email, String phone, String cpr, String uuid, long businessId, String startDate, String stopDate, String orgUnitId, String position, long activationPeriod) {}
	@Transactional
	@PostMapping("/create")
	public ResponseEntity<?> createEmployee(@RequestBody CreateEmployeeRecord createEmployeeRecord) {
		if (!StringUtils.hasText(createEmployeeRecord.uuid) && !StringUtils.hasText(createEmployeeRecord.cpr)) {
			log.warn("Either uuid or cpr number must be provided");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		Employee employee = new Employee();
		OrgUnit ou = orgUnitService.findOrgUnitByUuid(createEmployeeRecord.orgUnitId);
		if (ou == null) {
			log.warn("OrgUnit with uuid {} not found", createEmployeeRecord.uuid);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		Business business = businessService.findById(createEmployeeRecord.businessId);
		if (business != null) {
			employee.setBusiness(business);
			employee.setCvr(business.getCvr());
		}

		String startDateString = createEmployeeRecord.startDate.substring(0, 10);
		String stopDateString = createEmployeeRecord.stopDate.substring(0, 10);

		LocalDate startDate = LocalDate.parse(startDateString, DATE_FORMATTER);
		LocalDate stopDate = LocalDate.parse(stopDateString, DATE_FORMATTER);

		employee.setExtUuid(UUID.randomUUID().toString());
		employee.setName(createEmployeeRecord.name);
		employee.setEmail(createEmployeeRecord.email);
		employee.setPhone(createEmployeeRecord.phone);
		employee.setCpr(createEmployeeRecord.cpr);
		employee.setMitIdUuid(createEmployeeRecord.uuid);
		employee.setStartDate(startDate);
		employee.setStopDate(stopDate);
		employee.setOrgUnit(ou);
		employee.setPositionName(createEmployeeRecord.position);

		// This is a leader creating the employee, so they will automatically be leader approved and sent to admin approval
		employee.setApproved(ApproveStatus.LEADER_APPROVED);

		Employee save = employeeService.save(employee);
		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), save.getId(), save.getName(), LogAction.EMPLOYEE_CREATED, "Medarbejder blev oprettet", new EmployeeLogRecord(save.getName(), save.getEmail()));

		messageService.sendNotification(save, "OS2ILM: Ny adgang", cmsMessageBundle.getText("cms.employee.created"));

		return new ResponseEntity<>(HttpStatus.OK);
	}

	public record AdminCreateEmployeeRecord(String name, String email, String phone, String cpr, String uuid, long businessId, String startDate, String stopDate, String position, long archetype, String orgUnit, long activationId) {}
	@Transactional
	@PostMapping("/admin/create")
	public ResponseEntity<?> adminCreateEmployee(@RequestBody AdminCreateEmployeeRecord adminCreateEmployeeRecord) {
		if (!StringUtils.hasText(adminCreateEmployeeRecord.uuid) && !StringUtils.hasText(adminCreateEmployeeRecord.cpr)) {
			log.warn("Either uuid or cpr number must be provided");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		Employee employee = new Employee();

		String startDateString = adminCreateEmployeeRecord.startDate.substring(0, 10);
		String stopDateString = adminCreateEmployeeRecord.stopDate.substring(0, 10);

		LocalDate startDate = LocalDate.parse(startDateString, DATE_FORMATTER);
		LocalDate stopDate = LocalDate.parse(stopDateString, DATE_FORMATTER);

		employee.setExtUuid(UUID.randomUUID().toString());
		employee.setName(adminCreateEmployeeRecord.name);
		employee.setEmail(adminCreateEmployeeRecord.email);
		employee.setPhone(adminCreateEmployeeRecord.phone);
		employee.setCpr(adminCreateEmployeeRecord.cpr);
		employee.setMitIdUuid(adminCreateEmployeeRecord.uuid);
		employee.setStartDate(startDate);
		employee.setStopDate(stopDate);
		employee.setPositionName(adminCreateEmployeeRecord.position);

		OrgUnit orgUnit = orgUnitService.findOrgUnitByUuid(adminCreateEmployeeRecord.orgUnit);
		if (orgUnit != null) {
			employee.setOrgUnit(orgUnit);
		}

		Archetype archetype = archetypeService.findById(adminCreateEmployeeRecord.archetype);
		if (archetype != null) {
			// Set roles and groups on the employee
			if (archetype.getAdGroups() != null) {
				archetype.getAdGroups().forEach(employee::addAdGroup);
			}
			if (archetype.getUserRoles() != null) {
				archetype.getUserRoles().forEach(employee::addUserRole);
			}
		}

		Business business = businessService.findById(adminCreateEmployeeRecord.businessId);
		if (business != null) {
			employee.setBusiness(business);
			employee.setCvr(business.getCvr());
		}

		ActivationTimeFrame activationTimeFrame = activationTimeFrameService.findById(adminCreateEmployeeRecord.activationId);
		if (activationTimeFrame == null) {
			log.warn("The activation period does not exist");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		// This is an admin creating the employee, so they will automatically be approved
		employee.setApproved(ApproveStatus.ADMIN_APPROVED);
		employee.setActivationTimeFrame(activationTimeFrame);
		employee.setIntegrated(EmployeeStatus.AWAITING_FULL_INTEGRATION);
		Employee save = employeeService.save(employee);

		// Integrate employee into Sofd
		if (StringUtils.hasText(employee.getCpr())) {
			sofdService.sofdCreateAndUpdateHelper(save);
		}

		messageService.sendNotification(save, "OS2ILM: Ny adgang", cmsMessageBundle.getText("cms.employee.created"));

		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), save.getId(), save.getName(), LogAction.EMPLOYEE_CREATED, "Medarbejder blev oprettet af en administrator", new EmployeeLogRecord(save.getName(), save.getEmail()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/lock/{id}")
	public ResponseEntity<?> lockEmployee(@PathVariable long id) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		employee.setStopDate(LocalDate.now());

		Employee save = employeeService.save(employee);

		activeDirectorySyncService.setExpireDate(employee, LocalDate.now());

		String message = cmsMessageBundle.getText("cms.employee.closed");
		message = message.replace("PLACEHOLDER_RECIPIENT", employee.getName());
		messageService.sendNotification(employee, "OS2ILM: Adgang lukket", message);

		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), save.getId(), save.getName(), LogAction.EMPLOYEE_CLOSED, "Medarbejder blev lukket", new EmployeeLogRecord(save.getName(), save.getEmail()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	public record ExtendEmployeeRecord(long id, LocalDate stopDate) {}
	@PostMapping("/extend/")
	public ResponseEntity<?> extendEmployee(@RequestBody ExtendEmployeeRecord extendEmployeeRecord) {
		Employee employee = employeeService.findById(extendEmployeeRecord.id);
		if (employee == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		employee.setStopDate(extendEmployeeRecord.stopDate);
		if (employee.getApproved() == ApproveStatus.AWAITING_EXTENSION_APPROVAL) {
			if (securityUtil.isAdmin()) {
				employee.setApproved(ApproveStatus.ADMIN_APPROVED);
			}
			else {
				employee.setApproved(ApproveStatus.LEADER_APPROVED);
			}
		}

		Employee save = employeeService.save(employee);

		activeDirectorySyncService.setExpireDate(employee, extendEmployeeRecord.stopDate);

		String message = cmsMessageBundle.getText("cms.employee.extended");
		message = message.replace("PLACEHOLDER_RECIPIENT", employee.getName());
		message = message.replace("PLACEHOLDER_DATE", extendEmployeeRecord.stopDate.toString());
		messageService.sendNotification(employee, "OS2ILM: Adgang forlænget", message);

		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), save.getId(), save.getName(), LogAction.EXTENSION_APPROVED, "Medarbejder har fået deres anmodning om forlængelse godkendt", new EmployeeLogRecord(save.getName(), save.getEmail()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireEmployeeAdministratorOrHigher
	@PostMapping("/view")
	public DataTablesOutput<EmployeeView> employeeTable(@RequestBody DataTablesInput input) {
		List<String> cvrs = securityUtil.getCvrs();
		if (cvrs == null || cvrs.isEmpty()) {
			return employeeDatatableDao.findAll(input);
		}
		Specification<EmployeeView> spec = DatatableSpecBuilderUtil.getEmployeeViewSpecification(cvrs);

		return employeeDatatableDao.findAll(input, spec);
	}

	@PostMapping("/resetPassword/{id}")
	public ResponseEntity<?> resetPassword(@PathVariable long id) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		String password = RandomPasswordGeneratorUtil.generateUserFriendlyPassword(ilmConfiguration.getActiveDirectoryConfig().getPasswordLength());
		activeDirectorySyncService.setPassword(employee, password);

		String message = cmsMessageBundle.getText("cms.employee.password.reset");
		message = message.replace("PLACEHOLDER_RECIPIENT", employee.getName());
		message = message.replace("PLACEHOLDER_PASSWORD", password);
		messageService.sendNotification(employee, "OS2ILM: Kodeord nulstillet", message);

		String iden = securityUtil.getEmployee().getMitIdUuid() != null ? securityUtil.getEmployee().getMitIdUuid() : securityUtil.getEmployee().getCpr();
		auditLogService.log(iden, securityUtil.getName(), employee.getId(), employee.getName(), LogAction.EXTENSION_APPROVED, "Medarbejder har fået deres anmodning om forlængelse godkendt", new EmployeeLogRecord(employee.getName(), employee.getEmail()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/denyExtension/{id}")
	public ResponseEntity<?> denyExtension(@PathVariable long id) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		employee.setApproved(ApproveStatus.ADMIN_APPROVED);

		Employee save = employeeService.save(employee);

		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), save.getId(), save.getName(), LogAction.EXTENSION_DENIED, "Medarbejder har fået deres anmodning om forlængelse afvist", new EmployeeLogRecord(save.getName(), save.getEmail()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/unlock/")
	public ResponseEntity<?> unlockEmployee(@RequestBody ExtendEmployeeRecord extendEmployeeRecord) {
		Employee employee = employeeService.findById(extendEmployeeRecord.id);
		if (employee == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		employee.setStopDate(extendEmployeeRecord.stopDate);
		Employee save = employeeService.save(employee);

		activeDirectorySyncService.setExpireDate(employee, extendEmployeeRecord.stopDate);

		String message = cmsMessageBundle.getText("cms.employee.unlocked");
		message = message.replace("PLACEHOLDER_RECIPIENT", employee.getName());
		messageService.sendNotification(employee, "OS2ILM: Adgang genåbnet", message);

		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), save.getId(), save.getName(), LogAction.EMPLOYEE_REOPENED, "Medarbejder har fået deres AD-konti genåbnet", new EmployeeLogRecord(save.getName(), save.getEmail()));
		return new ResponseEntity<>(HttpStatus.OK);
	}


	public record EmployeeRecord(long employeeId) {}
	@PostMapping("/group/delete/{id}")
	public ResponseEntity<?> deleteGroup(@PathVariable long id, @RequestBody EmployeeRecord record) {
		employeeRelationshipService.deleteEmployeeGroups(record.employeeId, id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/role/delete/{id}")
	public ResponseEntity<?> deleteRole(@PathVariable long id, @RequestBody EmployeeRecord record) {
		employeeRelationshipService.deleteEmployeeRoles(record.employeeId, id);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
