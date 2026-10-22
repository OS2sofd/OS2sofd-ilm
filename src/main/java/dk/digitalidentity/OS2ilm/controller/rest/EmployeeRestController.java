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
import dk.digitalidentity.OS2ilm.model.entity.EmailTemplate;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplateType;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmployeeStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.ExchangeAccountStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.model.entity.enums.SofdSyncStatus;
import dk.digitalidentity.OS2ilm.model.exception.ActiveDirectoryIntegrationException;
import dk.digitalidentity.OS2ilm.model.exception.RoleCatalogIntegrationException;
import dk.digitalidentity.OS2ilm.model.role.RequireLeader;
import dk.digitalidentity.OS2ilm.model.role.RequireLeaderOrHigher;
import dk.digitalidentity.OS2ilm.model.role.RequireSystemAdministrator;
import dk.digitalidentity.OS2ilm.model.view.EmployeeView;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.ADQueueService;
import dk.digitalidentity.OS2ilm.service.ActivationTimeFrameService;
import dk.digitalidentity.OS2ilm.service.AdUserService;
import dk.digitalidentity.OS2ilm.service.ArchetypeService;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import dk.digitalidentity.OS2ilm.service.EmployeeRelationshipService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.MessageService;
import dk.digitalidentity.OS2ilm.service.OrgUnitService;
import dk.digitalidentity.OS2ilm.service.QueueHelperService;
import dk.digitalidentity.OS2ilm.service.EmailTemplateService;
import dk.digitalidentity.OS2ilm.util.DatatableSpecBuilderUtil;
import dk.digitalidentity.OS2ilm.util.RandomPasswordGeneratorUtil;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Hidden
@Slf4j
@RequestMapping("/rest/employee")
@RestController
@RequireLeader
@RequiredArgsConstructor
public class EmployeeRestController {
	private final EmployeeService employeeService;
	private final BusinessService businessService;
	private final EmployeeDatatableDao employeeDatatableDao;
	private final OrgUnitService orgUnitService;
	private final MessageService messageService;
	private final EmailTemplateService emailTemplateService;
	private final SecurityUtil securityUtil;
	private final IlmConfiguration ilmConfiguration;
	private final ArchetypeService archetypeService;
	private final ActivationTimeFrameService activationTimeFrameService;
	private final EmployeeRelationshipService employeeRelationshipService;
	private final AuditLogService auditLogService;
	private final ADQueueService aDQueueService;
	private final QueueHelperService queueHelperService;
	private final AdUserService adUserService;

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public record EditEmployeeRecord(String name, String email, String phone, long businessId, String startDate, String stopDate, String position, String cpr, List<Long> rolesToAdd, List<Long> groupsToAdd, List<Long> roleGroupsToAdd, List<Long> rolesToRemove, List<Long> groupsToRemove, List<Long> roleGroupsToRemove) {}
	public record EditEmployeeLogRecord(String name, String email, String phone, BusinessLogRecord business, String startDate, String stopDate, String position, List<String> addedRoles, List<String> addedGroups, List<String> addedRoleGroups, List<String> removedRoles, List<String> removedGroups, List<String> removedRoleGroups) {}
	public record BusinessLogRecord(Long id, String name) {}
	@Transactional
	@PatchMapping("/edit/{id}")
	public ResponseEntity<?> editEmployee(@PathVariable long id, @RequestBody EditEmployeeRecord editEmployeeRecord) throws RoleCatalogIntegrationException, ActiveDirectoryIntegrationException {
		Employee employee = employeeService.findById(id);
		Business business = businessService.findById(editEmployeeRecord.businessId);

		ResponseEntity<?> accessError = validateEditAccess(id, employee, business, editEmployeeRecord.businessId);
		if (accessError != null) {
			return accessError;
		}

		ResponseEntity<?> cprValidationError = validateCprChange(id, employee.getCpr(), editEmployeeRecord.cpr());
		if (cprValidationError != null) {
			return cprValidationError;
		}

		LocalDate startDate = LocalDate.parse(editEmployeeRecord.startDate, DATE_FORMATTER);
		LocalDate stopDate = LocalDate.parse(editEmployeeRecord.stopDate, DATE_FORMATTER);

		boolean stopDateChanged = (employee.getStopDate() == null && stopDate != null) || (employee.getStopDate() != null && !employee.getStopDate().isEqual(stopDate));
		boolean adChanges = stopDateChanged;
		employee.setStopDate(stopDate);

		boolean sofdChanges = employeeService.metadataChanges(editEmployeeRecord, employee, stopDateChanged, business, startDate);

		// Take care of the relation, i.e., add to each entity collection for persistence
		List<String> removedRoles = employeeRelationshipService.removeRoles(employee.getId(), editEmployeeRecord.rolesToRemove());
		List<String> removedRoleGroups = employeeRelationshipService.removeRoleGroups(employee.getId(), editEmployeeRecord.roleGroupsToRemove());
		List<String> removedGroups = employeeRelationshipService.removeGroups(employee.getId(), editEmployeeRecord.groupsToRemove());
		AddedRelations added = applyRelationAdditions(employee.getId(), editEmployeeRecord);

		Employee save = employeeService.save(employee);

		// Allows us to extend in the near future, but currently identical to editEmployeeRecord
		EditEmployeeLogRecord editLogRecord = new EditEmployeeLogRecord(
				editEmployeeRecord.name,
				editEmployeeRecord.email,
				editEmployeeRecord.phone,
				new BusinessLogRecord(business.getId(), business.getName()),
				editEmployeeRecord.startDate,
				editEmployeeRecord.stopDate,
				editEmployeeRecord.position,
				added.addedRoles(),
				added.addedGroups(),
				added.addedRoleGroups(),
				removedRoles,
				removedGroups,
				removedRoleGroups
		);

		String performerId = securityUtil.getPerformerId();
		String performerName = securityUtil.getName();
		auditLogService.log(performerId, performerName, employee.getId(), save.getName(), LogAction.EMPLOYEE_UPDATED, "Medarbejder blev opdateret", editLogRecord);

		// Now that every other field/role/group change above is applied and saved, swap the CPR by
		// deleting and recreating the employee - see changeEmployeeCpr for why this must run last.
		if (isCprChanging(editEmployeeRecord.cpr(), employee.getCpr())) {
			return changeEmployeeCpr(employee, editEmployeeRecord.cpr(), performerId, performerName);
		}

		notifyDownstreamOfEdit(employee, sofdChanges, adChanges, stopDate);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private ResponseEntity<?> validateEditAccess(long id, Employee employee, Business business, long businessId) {
		if (employee == null) {
			log.warn("Employee with id {} not found", id);
			return new ResponseEntity<>("Medarbejderen findes ikke", HttpStatus.BAD_REQUEST);
		}
		if (securityUtil.hasNoAccessToEmployee(employee)) {
			log.warn("Leader attempted to edit employee {} outside their business scope", id);
			return new ResponseEntity<>("Lederen har ingen adgang til denne medarbejder", HttpStatus.FORBIDDEN);
		}
		if (business == null) {
			log.warn("Agency with id {} not found", businessId);
			return new ResponseEntity<>("Virksomheden findes ikke", HttpStatus.BAD_REQUEST);
		}
		if (securityUtil.hasNoAccessToBusiness(businessId)) {
			log.warn("Leader attempted to assign employee {} to business {} outside their scope", id, businessId);
			return new ResponseEntity<>("Lederen har ingen adgang til denne virksomhed", HttpStatus.FORBIDDEN);
		}
		return null;
	}

	private record AddedRelations(List<String> addedRoles, List<String> addedGroups, List<String> addedRoleGroups) {}

	private AddedRelations applyRelationAdditions(long employeeId, EditEmployeeRecord editEmployeeRecord) {
		List<String> addedRoles = new ArrayList<>();
		List<String> addedGroups = new ArrayList<>();
		List<String> addedRoleGroups = new ArrayList<>();

		if (!editEmployeeRecord.rolesToAdd().isEmpty()) {
			addedRoles = employeeRelationshipService.updateEmployeeRoles(employeeId, editEmployeeRecord.rolesToAdd);
		}
		if (!editEmployeeRecord.groupsToAdd().isEmpty()) {
			addedGroups = employeeRelationshipService.updateEmployeeGroups(employeeId, editEmployeeRecord.groupsToAdd);
		}
		if (!editEmployeeRecord.roleGroupsToAdd().isEmpty()) {
			addedRoleGroups = employeeRelationshipService.updateEmployeeRoleGroups(employeeId, editEmployeeRecord.roleGroupsToAdd);
		}

		return new AddedRelations(addedRoles, addedGroups, addedRoleGroups);
	}

	private void notifyDownstreamOfEdit(Employee employee, boolean sofdChanges, boolean adChanges, LocalDate stopDate) {
		if (sofdChanges && StringUtils.hasText(employee.getCpr())) {
			queueHelperService.sofdHelper(employee.getId());
		}
		if (adChanges && employee.getUser() != null) {
			aDQueueService.addSetExpireDateToQueue(employee.getId(), stopDate);
		}
	}

	private boolean isCprChanging(String newCpr, String oldCpr) {
//		return StringUtils.hasText(newCpr) && !newCpr.equals(oldCpr);
		return false; // TODO: Tweak this before enabling feature
	}

	private ResponseEntity<?> validateCprChange(long employeeId, String oldCpr, String newCpr) {
		if (!isCprChanging(newCpr, oldCpr)) {
			return null;
		}
		if (!securityUtil.isAdmin()) {
			log.warn("Non-admin attempted to change CPR for employee {}", employeeId);
			return new ResponseEntity<>("Kun en administrator kan ændre CPR-nummer", HttpStatus.FORBIDDEN);
		}
		if (employeeService.findByCpr(newCpr) != null) {
			log.warn("Employee already exists with CPR: {}", newCpr);
			return new ResponseEntity<>("En medarbejder med dette CPR-nummer eksisterer allerede", HttpStatus.BAD_REQUEST);
		}
		return null;
	}

	private ResponseEntity<?> changeEmployeeCpr(Employee employee, String newCpr, String performerId, String performerName) {
		Employee current = employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(employee.getId());

		// Snapshot the data the new employee needs before the delete chain below mutates stopDate
		Employee newEmployee = new Employee();
		newEmployee.setName(current.getName());
		newEmployee.setEmail(current.getEmail());
		newEmployee.setPhone(current.getPhone());
		newEmployee.setMitIdUuid(current.getMitIdUuid());
		newEmployee.setBusiness(current.getBusiness());
		newEmployee.setCvr(current.getCvr());
		newEmployee.setPositionName(current.getPositionName());
		newEmployee.setStartDate(current.getStartDate());
		newEmployee.setStopDate(current.getStopDate());
		newEmployee.setAcceptedTos(current.isAcceptedTos());
		newEmployee.setActivationTimeFrame(current.getActivationTimeFrame());
		newEmployee.setRole(current.getRole());
		newEmployee.setOrgUnit(current.getOrgUnit());
		newEmployee.setBlocked(current.isBlocked());
		current.getAdGroups().forEach(newEmployee::addAdGroup);
		current.getUserRoles().forEach(newEmployee::addUserRole);
		current.getRoleGroups().forEach(newEmployee::addRoleGroup);
		// The old AD account's Exchange order doesn't carry over to the new account - if one was wanted, order it again
		newEmployee.setExchangeAccountStatus(current.getExchangeAccountStatus() == ExchangeAccountStatus.NONE ? ExchangeAccountStatus.NONE : ExchangeAccountStatus.NEEDS_ORDER);
		newEmployee.setCpr(newCpr);
		newEmployee.setExtUuid(UUID.randomUUID().toString());
		newEmployee.setExtAffiliationUuid(UUID.randomUUID().toString());
		newEmployee.setApproved(ApproveStatus.ADMIN_APPROVED);
		newEmployee.setIntegrated(EmployeeStatus.AWAITING_FULL_INTEGRATION);

		if (current.getUser() != null) {
			current.setStopDate(LocalDate.now());
			employeeService.save(current);
			aDQueueService.addDisableAccountToQueue(current.getId(), current.getUser().getId(), current.getName(), performerId, performerName);
		}
		else {
			queueHelperService.sofdHelperForDeletion(current.getId(), performerId, performerName);
		}

		Employee savedNewEmployee = employeeService.save(newEmployee);

		CreateEmployeeLogRecord logRecord = new CreateEmployeeLogRecord(
				savedNewEmployee.getId(),
				savedNewEmployee.getName(),
				savedNewEmployee.getEmail(),
				savedNewEmployee.getPhone(),
				"REDACTED",
				savedNewEmployee.getMitIdUuid(),
				savedNewEmployee.getBusiness() != null ? new BusinessLogRecord(savedNewEmployee.getBusiness().getId(), savedNewEmployee.getBusiness().getName()) : null,
				String.valueOf(savedNewEmployee.getStartDate()),
				String.valueOf(savedNewEmployee.getStopDate()),
				savedNewEmployee.getOrgUnit() != null ? new OrgunitLogRecord(savedNewEmployee.getOrgUnit().getUuid(), savedNewEmployee.getOrgUnit().getName()) : null,
				savedNewEmployee.getPositionName()
		);
		auditLogService.log(performerId, performerName, savedNewEmployee.getId(), savedNewEmployee.getName(), LogAction.EMPLOYEE_CREATED, "Medarbejder blev oprettet på ny grundet ændring af CPR-nummer", logRecord);

		// Trigger SOFD/AD provisioning for the new employee - same as when an admin approves a new employee
		queueHelperService.sofdHelper(savedNewEmployee.getId());

		EmailTemplate emailTemplate = emailTemplateService.getByType(EmailTemplateType.EMPLOYEE_CREATED);
		if (emailTemplate.isEnabled()) {
			EmailTemplateService.RenderedEmail email = emailTemplateService.render(emailTemplate, Map.of(EmailTemplatePlaceholder.RECIPIENT, savedNewEmployee.getName()));
			messageService.sendNotification(savedNewEmployee, email);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	public record CreateEmployeeRecord(String name, String email, String phone, String cpr, String uuid, long businessId, String startDate, String stopDate, String orgUnitId, String position) {}
	public record CreateEmployeeLogRecord(Long id, String name, String email, String phone, String cpr, String uuid, BusinessLogRecord business, String startDate, String stopDate, OrgunitLogRecord orgUnit, String position) {}
	public record OrgunitLogRecord(String uuid, String name) {}
	@Transactional
	@PostMapping("/create")
	public ResponseEntity<?> createEmployee(@RequestBody CreateEmployeeRecord createEmployeeRecord) {
		if (!StringUtils.hasText(createEmployeeRecord.uuid) && !StringUtils.hasText(createEmployeeRecord.cpr)) {
			log.warn("Either uuid or cpr number must be provided");
			return new ResponseEntity<>("Du skal angive enten et CPR-nummer eller en UUID", HttpStatus.BAD_REQUEST);
		}

		if (securityUtil.hasNoAccessToBusiness(createEmployeeRecord.businessId)) {
			log.warn("Leader attempted to create employee for business {} outside their scope", createEmployeeRecord.businessId);
			return new ResponseEntity<>("Lederen har ikke adgang til denne medarbejder", HttpStatus.FORBIDDEN);
		}

		Employee employee = new Employee();
		OrgUnit ou = orgUnitService.findOrgUnitByUuid(createEmployeeRecord.orgUnitId);
		if (ou != null) {
			employee.setOrgUnit(ou);
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

		// Set metadata
		employeeService.updateEmployee(employee, null, createEmployeeRecord, startDate, stopDate);

		// This is a leader creating the employee, so they will automatically be leader approved and sent to admin approval
		employee.setApproved(ApproveStatus.LEADER_APPROVED);

		Employee save = employeeService.save(employee);

		CreateEmployeeLogRecord logRecord = new CreateEmployeeLogRecord(
				save.getId(),
				createEmployeeRecord.name,
				createEmployeeRecord.email,
				createEmployeeRecord.phone,
				"REDACTED",
				createEmployeeRecord.uuid,
				business != null ? new BusinessLogRecord(business.getId(), business.getName()) : null,
				createEmployeeRecord.startDate,
				createEmployeeRecord.stopDate,
				ou != null ? new OrgunitLogRecord(ou.getUuid(), ou.getName()) : null,
				createEmployeeRecord.position
		);
		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), save.getId(), save.getName(), LogAction.EMPLOYEE_CREATED, "Medarbejder blev oprettet", logRecord);

		EmailTemplate emailTemplate = emailTemplateService.getByType(EmailTemplateType.EMPLOYEE_CREATED);
		if (emailTemplate.isEnabled()) {
			EmailTemplateService.RenderedEmail email = emailTemplateService.render(emailTemplate, Map.of(EmailTemplatePlaceholder.RECIPIENT, employee.getName()));
			messageService.sendNotification(save, email);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	public record AdminCreateEmployeeRecord(String name, String email, String phone, String cpr, String uuid, long businessId, String startDate, String stopDate, String position, long archetype, String orgUnit, long activationId, boolean enableOrdering) {}
	public record AdminCreateEmployeeLogRecord(Long id, String name, String email, String phone, String cpr, String uuid, BusinessLogRecord business, String startDate, String stopDate, String position, ArchetypeRecord archetype, OrgunitLogRecord orgUnit) {}
	public record ArchetypeRecord(Long id, String name) {}
	@Transactional
	@PostMapping("/admin/create")
	public ResponseEntity<?> adminCreateEmployee(@RequestBody AdminCreateEmployeeRecord adminCreateEmployeeRecord) {
		if (!StringUtils.hasText(adminCreateEmployeeRecord.uuid) && !StringUtils.hasText(adminCreateEmployeeRecord.cpr)) {
			log.warn("Either uuid or cpr number must be provided");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		// Validate cpr and mitid and check if the user already exists
		Employee existingEmployee = null;

		if (StringUtils.hasText(adminCreateEmployeeRecord.cpr)) {
			existingEmployee = employeeService.findByCpr(adminCreateEmployeeRecord.cpr);
			if (existingEmployee != null) {
				log.warn("Employee already exists with CPR: {}", adminCreateEmployeeRecord.cpr);
				return new ResponseEntity<>("En medarbejder med dette CPR-nummer eksisterer allerede", HttpStatus.BAD_REQUEST);
			}
		}

		if (StringUtils.hasText(adminCreateEmployeeRecord.uuid)) {
			existingEmployee = employeeService.findByMitIdUuid(adminCreateEmployeeRecord.uuid);
			if (existingEmployee != null) {
				log.warn("Employee already exists with MitID UUID: {}", adminCreateEmployeeRecord.uuid);
				return new ResponseEntity<>("En medarbejder med dette MitID UUID eksisterer allerede", HttpStatus.BAD_REQUEST);
			}
		}

		Employee employee = new Employee();

		String startDateString = adminCreateEmployeeRecord.startDate.substring(0, 10);
		String stopDateString = adminCreateEmployeeRecord.stopDate != null ? adminCreateEmployeeRecord.stopDate.substring(0, 10) : null;

		LocalDate startDate = LocalDate.parse(startDateString, DATE_FORMATTER);
		LocalDate stopDate = stopDateString != null ? LocalDate.parse(stopDateString, DATE_FORMATTER) : null;

		// Set metadata
		employeeService.updateEmployee(employee, adminCreateEmployeeRecord, null, startDate, stopDate);

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
			if (archetype.getRoleGroups() != null) {
				archetype.getRoleGroups().forEach(employee::addRoleGroup);
			}
		}

		Business business = businessService.findById(adminCreateEmployeeRecord.businessId);
		if (business != null) {
			employee.setBusiness(business);
			employee.setCvr(business.getCvr());
		}

		ActivationTimeFrame activationTimeFrame = activationTimeFrameService.findById(adminCreateEmployeeRecord.activationId);

		// This is an admin creating the employee, so they will automatically be approved
		employee.setApproved(ApproveStatus.ADMIN_APPROVED);
		employee.setActivationTimeFrame(activationTimeFrame);
		employee.setIntegrated(EmployeeStatus.AWAITING_FULL_INTEGRATION);
		employee.setExchangeAccountStatus(adminCreateEmployeeRecord.enableOrdering ? ExchangeAccountStatus.NEEDS_ORDER : ExchangeAccountStatus.NONE);
		Employee save = employeeService.save(employee);

		EmailTemplate emailTemplate = emailTemplateService.getByType(EmailTemplateType.EMPLOYEE_CREATED);
		if (emailTemplate.isEnabled()) {
			EmailTemplateService.RenderedEmail email = emailTemplateService.render(emailTemplate, Map.of(EmailTemplatePlaceholder.RECIPIENT, employee.getName()));
			messageService.sendNotification(save, email);
		}

		AdminCreateEmployeeLogRecord logRecord = new AdminCreateEmployeeLogRecord(
				save.getId(),
				adminCreateEmployeeRecord.name,
				adminCreateEmployeeRecord.email,
				adminCreateEmployeeRecord.phone,
				"REDACTED",
				adminCreateEmployeeRecord.uuid,
				business != null ? new BusinessLogRecord(business.getId(), business.getName()) : null,
				adminCreateEmployeeRecord.startDate,
				adminCreateEmployeeRecord.stopDate,
				adminCreateEmployeeRecord.position,
				archetype != null ? new ArchetypeRecord(archetype.getId(), archetype.getName()) : null,
				orgUnit != null ? new OrgunitLogRecord(orgUnit.getUuid(), orgUnit.getName()) : null
		);

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), save.getId(), save.getName(), LogAction.EMPLOYEE_CREATED, "Medarbejder blev oprettet af en administrator", logRecord);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	public record LockEmployeeLogRecord(Long id, String name, String email, LocalDate lockDate) {}
	@PostMapping("/lock/{id}")
	public ResponseEntity<?> lockEmployee(@PathVariable long id) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		if (securityUtil.hasNoAccessToEmployee(employee)) {
			log.warn("Leader attempted to lock employee {} outside their business scope", id);
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		if (employee.getUser() == null) {
			log.warn("Leader attempted to lock employee {} who has no AD account", id);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		employee.setStopDate(LocalDate.now());
		employeeService.save(employee);

		aDQueueService.addSetExpireDateToQueue(id, LocalDate.now());
		queueHelperService.sofdHelper(id);

		EmailTemplate emailTemplate = emailTemplateService.getByType(EmailTemplateType.EMPLOYEE_CLOSED);
		if (emailTemplate.isEnabled()) {
			EmailTemplateService.RenderedEmail email = emailTemplateService.render(emailTemplate, Map.of(EmailTemplatePlaceholder.RECIPIENT, employee.getName()));
			messageService.sendNotification(employee, email);
		}

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), employee.getId(), employee.getName(), LogAction.EMPLOYEE_CLOSED, "Medarbejder blev lukket", new LockEmployeeLogRecord(employee.getId(), employee.getName(), employee.getEmail(), LocalDate.now()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	public record ExtendEmployeeRecord(long id, LocalDate stopDate) {}
	public record ExtendEmployeeLogRecord(Long id, String name, String email, LocalDate stopDate) {}
	@PostMapping("/extend")
	public ResponseEntity<?> extendEmployee(@RequestBody ExtendEmployeeRecord extendEmployeeRecord) {
		Employee employee = employeeService.findById(extendEmployeeRecord.id);
		if (employee == null) {
			return new ResponseEntity<>("Medarbejder findes ikke", HttpStatus.NOT_FOUND);
		}
		if (securityUtil.hasNoAccessToEmployee(employee)) {
			log.warn("Leader attempted to extend employee {} outside their business scope", extendEmployeeRecord.id);
			return new ResponseEntity<>("Lederen har ingen adgang til denne medarbejder", HttpStatus.FORBIDDEN);
		}
		if (employee.getUser() == null) {
			log.warn("Leader attempted to extend employee {} who has no AD account", extendEmployeeRecord.id);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		if (employee.getApproved() == ApproveStatus.AWAITING_EXTENSION_APPROVAL) {
			if (securityUtil.isAdmin()) {
				employee.setApproved(ApproveStatus.ADMIN_APPROVED);
			}
			else {
				employee.setApproved(ApproveStatus.LEADER_APPROVED);
			}
		}

		Employee save = employeeService.save(employee);

		// Queue the expireDate and sofd update
		aDQueueService.addSetExpireDateToQueue(extendEmployeeRecord.id, extendEmployeeRecord.stopDate);
		queueHelperService.sofdHelper(extendEmployeeRecord.id);

		EmailTemplate emailTemplate = emailTemplateService.getByType(EmailTemplateType.EMPLOYEE_EXTENDED);
		if (emailTemplate.isEnabled()) {
			EmailTemplateService.RenderedEmail email = emailTemplateService.render(emailTemplate, Map.of(
					EmailTemplatePlaceholder.RECIPIENT, employee.getName(),
					EmailTemplatePlaceholder.DATE, extendEmployeeRecord.stopDate.toString()));
			messageService.sendNotification(employee, email);
		}

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), save.getId(), save.getName(), LogAction.EXTENSION_APPROVED, "Medarbejder har fået deres ad-konto forlænget", new ExtendEmployeeLogRecord(save.getId(), save.getName(), save.getEmail(), extendEmployeeRecord.stopDate()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireLeaderOrHigher
	@PostMapping("/view")
	public DataTablesOutput<EmployeeView> employeeTable(@RequestBody DataTablesInput input) {
		List<String> cvrs = securityUtil.getCvrs();
		if (cvrs == null || cvrs.isEmpty()) {
			return employeeDatatableDao.findAll(input);
		}
		Specification<EmployeeView> spec = DatatableSpecBuilderUtil.getEmployeeViewSpecification(cvrs);

		return employeeDatatableDao.findAll(input, spec);
	}

	public record ResetPasswordLogRecord(Long id, String name, String email, String password) {}
	@PostMapping("/resetPassword/{id}")
	public ResponseEntity<?> resetPassword(@PathVariable long id) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		if (securityUtil.hasNoAccessToEmployee(employee)) {
			log.warn("Leader attempted to reset password for employee {} outside their business scope", id);
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		// Notification is sent from ActiveDirectorySyncService.setPassword after AD confirms success, so that
		// the user does not receive a password email if AD ultimately rejects the change.
		String password = RandomPasswordGeneratorUtil.generateUserFriendlyPassword(ilmConfiguration.getActiveDirectoryConfig().getPasswordLength());
		aDQueueService.addSetPasswordToQueue(id, password);

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), employee.getId(), employee.getName(), LogAction.RESET_PASSWORD, "Medarbejder har fået deres kodeord nulstillet", new ResetPasswordLogRecord(employee.getId(), employee.getName(), employee.getEmail(), "REDACTED"));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	public record OrderExchangeLogRecord(Long id, String name, String email) {}
	@Transactional
	@PostMapping("/orderExchange/{id}")
	public ResponseEntity<?> orderExchangeAccount(@PathVariable long id) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			return new ResponseEntity<>("Medarbejderen findes ikke", HttpStatus.NOT_FOUND);
		}
		if (securityUtil.hasNoAccessToEmployee(employee)) {
			log.warn("Leader attempted to order an Exchange account for employee {} outside their business scope", id);
			return new ResponseEntity<>("Lederen har ingen adgang til at redigere i medarbejderen", HttpStatus.FORBIDDEN);
		}
		if (employee.getExchangeAccountStatus() == ExchangeAccountStatus.HAS_ACCOUNT || employee.getExchangeAccountStatus() == ExchangeAccountStatus.NEEDS_ORDER) {
			log.warn("Attempted to order an Exchange account for employee {} who already has one or has a pending order", id);
			return new ResponseEntity<>("Medarbejderen har allerede bestilt en mailkonto", HttpStatus.BAD_REQUEST);
		}

		AdUser user = employee.getUser();
		if (user == null) {
			return new ResponseEntity<>("Medarbejderen har ingen ad-konto", HttpStatus.NOT_FOUND);
		}

		// Mark the employee and user so the scheduled SOFD task picks them up and places the order
		employee.setExchangeAccountStatus(ExchangeAccountStatus.NEEDS_ORDER);
		employeeService.save(employee);

		if (user.getSofdSyncStatus() == SofdSyncStatus.EXCHANGE_FAILED) {
			user.setSofdSyncStatus(SofdSyncStatus.SYNCED);
			adUserService.save(user);
		}

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), employee.getId(), employee.getName(), LogAction.EXCHANGE_ORDER_REQUESTED, "Der er bestilt en mailkonto til medarbejderen", new OrderExchangeLogRecord(employee.getId(), employee.getName(), employee.getEmail()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	public record DenyExtensionLogRecord(String name, String email, String status) {}
	@PostMapping("/denyExtension/{id}")
	public ResponseEntity<?> denyExtension(@PathVariable long id) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		if (securityUtil.hasNoAccessToEmployee(employee)) {
			log.warn("Leader attempted to deny extension for employee {} outside their business scope", id);
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		employee.setApproved(ApproveStatus.DENIED);

		Employee save = employeeService.save(employee);

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), save.getId(), save.getName(), LogAction.EXTENSION_DENIED, "Medarbejder har fået deres anmodning om forlængelse afvist", new DenyExtensionLogRecord(save.getName(), save.getEmail(), "Afvist"));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/unlock")
	public ResponseEntity<?> unlockEmployee(@RequestBody ExtendEmployeeRecord extendEmployeeRecord) {
		Employee employee = employeeService.findById(extendEmployeeRecord.id);
		if (employee == null) {
			return new ResponseEntity<>("Kunne ikke finde en medarbejder med denne ID", HttpStatus.NOT_FOUND);
		}
		if (securityUtil.hasNoAccessToEmployee(employee)) {
			log.warn("Leader attempted to unlock employee {} outside their business scope", extendEmployeeRecord.id);
			return new ResponseEntity<>("Lederen har ikke adgang til denne medarbejder", HttpStatus.FORBIDDEN);
		}
		if (employee.getUser() == null) {
			log.warn("Leader attempted to unlock employee {} who has no AD account", extendEmployeeRecord.id);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		aDQueueService.addSetExpireDateToQueue(extendEmployeeRecord.id, extendEmployeeRecord.stopDate);

		EmailTemplate emailTemplate = emailTemplateService.getByType(EmailTemplateType.EMPLOYEE_UNLOCKED);
		if (emailTemplate.isEnabled()) {
			EmailTemplateService.RenderedEmail email = emailTemplateService.render(emailTemplate, Map.of(EmailTemplatePlaceholder.RECIPIENT, employee.getName()));
			messageService.sendNotification(employee, email);
		}

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), employee.getId(), employee.getName(), LogAction.EMPLOYEE_REOPENED, "Medarbejder har fået deres AD-konti genåbnet", new ExtendEmployeeLogRecord(employee.getId(), employee.getName(), employee.getEmail(), extendEmployeeRecord.stopDate()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireSystemAdministrator
	@DeleteMapping("/delete/{id}")
	public ResponseEntity<?> deleteEmployee(@PathVariable long id) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			return new ResponseEntity<>("Medarbejderen findes ikke", HttpStatus.NOT_FOUND);
		}

		String performerId = securityUtil.getPerformerId();
		String performerName = securityUtil.getName();

		if (employee.getUser() != null) {
			employee.setStopDate(LocalDate.now());
			employeeService.save(employee);

			aDQueueService.addDeleteAccountToQueue(employee.getId(), employee.getUser().getId(), employee.getName(), performerId, performerName);
		}
		else {
			queueHelperService.sofdHelperForDeletion(employee.getId(), performerId, performerName);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
