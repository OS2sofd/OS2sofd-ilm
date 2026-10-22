package dk.digitalidentity.OS2ilm.controller.rest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import dk.digitalidentity.OS2ilm.model.datatable.dao.ApprovedEmployeeDatatableDao;
import dk.digitalidentity.OS2ilm.model.entity.Archetype;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.Leader;
import dk.digitalidentity.OS2ilm.model.entity.OrgUnit;
import dk.digitalidentity.OS2ilm.model.entity.BusinessEmploymentSettings;
import dk.digitalidentity.OS2ilm.model.entity.enums.AccountNameStrategy;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import dk.digitalidentity.OS2ilm.model.entity.EmailTemplate;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplateType;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmployeeStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.ExchangeAccountStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.model.exception.InvalidAccountNameException;
import dk.digitalidentity.OS2ilm.model.role.RequireLeader;
import dk.digitalidentity.OS2ilm.model.role.RequireLeaderOrHigher;
import dk.digitalidentity.OS2ilm.model.view.ApprovedEmployeeView;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.AccountNameValidator;
import dk.digitalidentity.OS2ilm.service.AdUserService;
import dk.digitalidentity.OS2ilm.service.ArchetypeService;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.MessageService;
import dk.digitalidentity.OS2ilm.service.OrgUnitService;
import dk.digitalidentity.OS2ilm.service.QueueHelperService;
import dk.digitalidentity.OS2ilm.service.EmailTemplateService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@Slf4j
@RequestMapping("/rest")
@RestController
@RequiredArgsConstructor
@RequireLeader
public class ApprovalRestController {
	private final EmployeeService employeeService;
	private final SecurityUtil securityUtil;
	private final ApprovedEmployeeDatatableDao approvedEmployeeDatatableDao;
	private final OrgUnitService orgUnitService;
	private final ArchetypeService archetypeService;
	private final BusinessService businessService;
	private final AuditLogService auditLogService;

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private final EmailTemplateService emailTemplateService;
	private final MessageService messageService;
	private final QueueHelperService queueHelperService;
	private final AccountNameValidator accountNameValidator;
	private final AdUserService adUserService;

	record EmployeeRecord(Long id, String name, String email, String status) {}
	@PostMapping("/approve/{id}")
	public ResponseEntity<?> approveEmployee(@PathVariable long id) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			log.warn("Employee with id {} not found", id);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		if (securityUtil.hasNoAccessToEmployee(employee)) {
			log.warn("Leader attempted to approve employee {} outside their business scope", id);
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		employee.setApproved(ApproveStatus.LEADER_APPROVED);
		employeeService.save(employee);

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), id, employee.getName(), LogAction.EMPLOYEE_LEADER_APPROVED, "Medarbejder er blevet godkendt af en leder", new EmployeeRecord(employee.getId(), employee.getName(), employee.getEmail(), employee.getApproved().toString()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/deny/{id}")
	public ResponseEntity<?> denyEmployee(@PathVariable long id) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			log.warn("Employee with id {} not found", id);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		if (securityUtil.hasNoAccessToEmployee(employee)) {
			log.warn("Leader attempted to deny employee {} outside their business scope", id);
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		employee.setApproved(ApproveStatus.DENIED);
		employeeService.save(employee);

		// This will always be a leader as admins deny using a different endpoint
		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), id, employee.getName(), LogAction.EMPLOYEE_DENIED, "Medarbejder er blevet afvist", new EmployeeRecord(employee.getId(), employee.getName(), employee.getEmail(), employee.getApproved().toString()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireLeaderOrHigher
	@PostMapping("/approve/view")
	public DataTablesOutput<ApprovedEmployeeView> approvedTable(@RequestBody DataTablesInput input, @RequestParam(defaultValue = "false") boolean hideRejected) {
		List<String> cvrs = securityUtil.getCvrs();

		boolean isLeader = securityUtil.isLeader();
		boolean isAdmin = securityUtil.isAdmin();

		Specification<ApprovedEmployeeView> spec;
		if (cvrs == null || cvrs.isEmpty()) {
			if (isLeader && !isAdmin) {
				spec = DatatableSpecBuilderUtil.getLeaderSpecificationWithoutCVR();
			} else if (isAdmin) {
				spec = DatatableSpecBuilderUtil.getAdminSpecificationWithoutCVR();
			} else {
				spec = null;
			}
		} else {
			if (isLeader && !isAdmin) {
				spec = DatatableSpecBuilderUtil.getLeaderSpecification(cvrs);
			} else if (isAdmin) {
				spec = DatatableSpecBuilderUtil.getAdminSpecification(cvrs);
			} else {
				spec = DatatableSpecBuilderUtil.getApprovedEmployeeViewSpecification(cvrs);
			}
		}

		if (hideRejected) {
			Specification<ApprovedEmployeeView> excludeDenied = (root, query, cb) -> cb.notEqual(root.get("approval"), ApproveStatus.DENIED);
			spec = spec == null ? excludeDenied : spec.and(excludeDenied);
		}

		return spec == null ? approvedEmployeeDatatableDao.findAll(input) : approvedEmployeeDatatableDao.findAll(input, spec);
	}

	public record ApproveEmployeeRecord(boolean approved, boolean denied, Long archetype, String ou, String startDate, String stopDate, Long group, Long activationPeriod, String businessName, boolean enableOrdering, String manualAccountName) {}
	public record ApproveEmployeeLogRecord(Long id, String name, String approved, ArchetypeRecord archetype, BusinessLogRecord business, OrgunitLogRecord ou, LocalDate startDate, LocalDate stopDate) {}
	public record OrgunitLogRecord(String uuid, String name) {}
	public record BusinessLogRecord(Long id, String name) {}
	public record ArchetypeRecord(Long id, String name) {}
	@PostMapping("/approval/{id}")
	public ResponseEntity<?> adminApproval(@RequestBody ApproveEmployeeRecord approveEmployeeRecord, @PathVariable Long id) {
		Employee employee = employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(id);
		if (employee == null) {
			log.warn("The employee does not exist");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		if (employee.getApproved() == ApproveStatus.ADMIN_APPROVED) {
			log.warn("Employee {} is already approved", id);
			return new ResponseEntity<>(HttpStatus.CONFLICT);
		}

		boolean sendMailToLeader = employee.getApproved() != ApproveStatus.LEADER_APPROVED;

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
		String stopDateString = approveEmployeeRecord.stopDate != null ? approveEmployeeRecord.stopDate.substring(0, 10) : null;

		LocalDate startDate = LocalDate.parse(startDateString, DATE_FORMATTER);
		LocalDate stopDate = stopDateString != null ? LocalDate.parse(stopDateString, DATE_FORMATTER) : null;

		employee.setStartDate(startDate);
		employee.setStopDate(stopDate);

		OrgUnit orgUnit = orgUnitService.findOrgUnitByUuid(approveEmployeeRecord.ou);
		if (orgUnit == null) {
			log.warn("The orgunit does not exist");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		employee.setOrgUnit(orgUnit);
		employee.setIntegrated(EmployeeStatus.AWAITING_FULL_INTEGRATION);
		employee.setExchangeAccountStatus(approveEmployeeRecord.enableOrdering ? ExchangeAccountStatus.NEEDS_ORDER : ExchangeAccountStatus.NONE);

		Archetype archetype = approveEmployeeRecord.archetype() != null ? archetypeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(approveEmployeeRecord.archetype) : null;

		if (archetype != null) {
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

		Business business = businessService.findByIdWithLeadersLoaded(employee.getBusiness().getId());
		BusinessEmploymentSettings settings = business.getBusinessEmploymentSettings();
		AccountNameStrategy nameStrategy = settings != null ? settings.getNameStrategy() : null;

		if (nameStrategy == AccountNameStrategy.CUSTOM) {
			if (!StringUtils.hasText(approveEmployeeRecord.manualAccountName)) {
				log.warn("Employee {} uses CUSTOM naming but no manualAccountName was supplied", id);
				return new ResponseEntity<>("Du skal angive et brugernavn", HttpStatus.BAD_REQUEST);
			}
			try {
				accountNameValidator.validate(approveEmployeeRecord.manualAccountName);
			} catch (InvalidAccountNameException ex) {
				return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
			}
			if (adUserService.findByName(approveEmployeeRecord.manualAccountName) != null) {
				return new ResponseEntity<>("Brugernavnet er allerede i brug", HttpStatus.BAD_REQUEST);
			}
			employee.setPendingAccountName(approveEmployeeRecord.manualAccountName);
		}

		Employee savedEmployee = employeeService.save(employee);

		// Set a prefix if it doesn't exist yet (can happen when registering with a new business) - only relevant for the legacy PREFIXED strategy
		if (nameStrategy == AccountNameStrategy.PREFIXED && StringUtils.hasText(approveEmployeeRecord.businessName) && !StringUtils.hasText(business.getPrefix())) {
			business.setPrefix(approveEmployeeRecord.businessName);
			businessService.save(business);
		}

		// Sync to SOFD
		queueHelperService.sofdHelper(savedEmployee.getId());

		ApproveEmployeeLogRecord approveLogRecord = new ApproveEmployeeLogRecord(
				savedEmployee.getId(),
				savedEmployee.getName(),
				savedEmployee.getApproved().toString(),
				archetype != null ? new ArchetypeRecord(archetype.getId(), archetype.getName()) : null,
				business != null ? new BusinessLogRecord(business.getId(), business.getName()) : null,
				new OrgunitLogRecord(orgUnit.getUuid(), orgUnit.getName()),
				savedEmployee.getStartDate(),
				savedEmployee.getStopDate()
		);
		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), id, savedEmployee.getName(), LogAction.EMPLOYEE_ADMIN_APPROVED, "Medarbejder er blevet godkendt af en administrator", approveLogRecord);

		EmailTemplate emailTemplate = emailTemplateService.getByType(EmailTemplateType.ADMIN_APPROVED);
		if (sendMailToLeader && emailTemplate.isEnabled()) {
			for (Leader leader : business.getLeaders()) {
				EmailTemplateService.RenderedEmail email = emailTemplateService.render(emailTemplate, Map.of(EmailTemplatePlaceholder.RECIPIENT, leader.getName()));
				messageService.sendNotificationUsingMail(leader.getMail(), email);
			}
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
