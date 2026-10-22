package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.controller.rest.EmployeeRestController;
import dk.digitalidentity.OS2ilm.model.dao.EmployeeDao;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.Leader;
import dk.digitalidentity.OS2ilm.model.entity.OrgUnit;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import dk.digitalidentity.OS2ilm.model.entity.EmailTemplate;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplateType;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmployeeStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.ExchangeAccountStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {
	private final EmployeeDao employeeDao;
	private final IlmConfiguration configuration;
	private final EmployeeDeletionService employeeDeletionService;
	private final MessageService messageService;
	private final EmailTemplateService emailTemplateService;

	public List<Employee> findAll() {
		return employeeDao.findAll();
	}

	// Clears the cpr, which the query-level deleted=false restriction already treats as free for reuse,
	// so it also stops occupying the DB's unique_employee_cpr constraint - otherwise a later create/CPR-change
	// reusing this CPR would pass the (now soft-delete-blind) uniqueness check here and then fail on save().
	public void delete(Employee employee) {
		employee.setDeleted(true);
		employee.setCpr(null);
		employeeDao.save(employee);
	}

	public Employee save(Employee employee) {
		return employeeDao.save(employee);
	}

	public Employee findByIdWithUserRolesAndAdGroupsAndRoleGroups(Long id) {
		return employeeDao.findByIdWithUserRolesAndAdGroupsAndRoleGroups(id);
	}

	public Employee findByCprWithUserRolesAndAdGroupsAndRoleGroups(String cpr) {
		return employeeDao.findByCprWithUserRolesAndAdGroupsAndRoleGroups(cpr);
	}

	@Transactional
	public Employee findById(Long id) {
		return employeeDao.findById(id).orElse(null);
	}

	public List<Employee> findAllEmployeesByBusiness(Business agency) {
		return employeeDao.findAllEmployeesByBusiness(agency);
	}

	public List<Employee> findByCvr(String cvr) {
		return employeeDao.findByCvr(cvr);
	}

	@Transactional
	public void updateCvrForEmployees(Business business, String newCvr) {
		List<Employee> employees = employeeDao.findAllEmployeesByBusiness(business);
		employees.forEach(employee -> employee.setCvr(newCvr));
		if (!employees.isEmpty()) {
			employeeDao.saveAll(employees);
		}
	}

	public List<Employee> findByCvrIn(List<String> cvrs) {
		if (cvrs == null || cvrs.isEmpty()) {
			return List.of();
		}
		return employeeDao.findByCvrIn(cvrs);
	}

	public List<Employee> findByOrgUnit(OrgUnit orgUnit) {
		return employeeDao.findByOrgUnit(orgUnit);
	}

	public void saveAll(List<Employee> employees) {
		employeeDao.saveAll(employees);
	}

	public Employee findByCpr(String cpr) {
		return employeeDao.findByCpr(cpr);
	}

	public Employee findByMitIdUuid(String mitIdUuid) {
		return employeeDao.findByMitIdUuid(mitIdUuid);
	}

	public List<Employee> findByIntegratedAndLoaded(EmployeeStatus status) {
		return employeeDao.findByIntegratedAndLoaded(status);
	}

	public List<Employee> findByBusinessAndRole(Business business, String role) {
		return employeeDao.findByBusinessAndRole(business, role);
	}

	public List<Employee> findByExchangeAccountStatus(ExchangeAccountStatus exchangeAccountStatus) {
		return employeeDao.findByExchangeAccountStatus(exchangeAccountStatus);
	}

	public List<Employee> findByLastLoginBefore(LocalDateTime date) {
		return employeeDao.findByLastLoginBefore(date);
	}

	@Transactional
	public void cleanUpOldUsers() {
		List<Employee> employees = findByLastLoginBefore(LocalDateTime.now().minusMonths(configuration.getScheduled().cleanUpOldUsersInterval));

		if (!employees.isEmpty()) {
			employeeDeletionService.deleteAll(employees);
			employeeDeletionService.deleteAllInAud(employees);
		}

		log.debug("Deleting a total of {} employees", employees.size());
	}

	public List<Employee> findEmployeesWithPendingApprovals(List<ApproveStatus> statuses) {
		return employeeDao.findEmployeesWithPendingApprovals(statuses);
	}

	public boolean metadataChanges(EmployeeRestController.EditEmployeeRecord editEmployeeRecord, Employee employee, boolean sofdChanges, Business business, LocalDate startDate) {
		if (!editEmployeeRecord.name().equals(employee.getName())) {
			employee.setName(editEmployeeRecord.name());
			sofdChanges = true;
		}

		if (!editEmployeeRecord.email().equals(employee.getEmail())) {
			employee.setEmail(editEmployeeRecord.email());
			sofdChanges = true;
		}

		if (!editEmployeeRecord.phone().equals(employee.getPhone())) {
			employee.setPhone(editEmployeeRecord.phone());
			sofdChanges = true;
		}

		if (editEmployeeRecord.businessId() != business.getId()) {
			employee.setBusiness(business);
			sofdChanges = true;
		}

		if (!editEmployeeRecord.position().equals(employee.getPositionName())) {
			employee.setPositionName(editEmployeeRecord.position());
			sofdChanges = true;
		}

		if (!startDate.equals(employee.getStartDate())) {
			employee.setStartDate(startDate);
			sofdChanges = true;
		}

		return sofdChanges;
	}

	public void updateEmployee(Employee employee, EmployeeRestController.AdminCreateEmployeeRecord adminCreateEmployeeRecord, EmployeeRestController.CreateEmployeeRecord createEmployeeRecord,LocalDate startDate, LocalDate stopDate) {
		employee.setExtUuid(UUID.randomUUID().toString());
		employee.setExtAffiliationUuid(UUID.randomUUID().toString());

		if (adminCreateEmployeeRecord != null) {
			setMetadata(employee, startDate, stopDate, adminCreateEmployeeRecord.name(), adminCreateEmployeeRecord.email(), adminCreateEmployeeRecord.phone(), adminCreateEmployeeRecord.cpr(), adminCreateEmployeeRecord.uuid(), adminCreateEmployeeRecord.position());
		} else {
			setMetadata(employee, startDate, stopDate, createEmployeeRecord.name(), createEmployeeRecord.email(), createEmployeeRecord.phone(), createEmployeeRecord.cpr(), createEmployeeRecord.uuid(), createEmployeeRecord.position());
		}
	}

	private void setMetadata(Employee employee, LocalDate startDate, LocalDate stopDate, String name, String email, String phone, String cpr, String uuid, String position) {
		employee.setName(name);
		employee.setEmail(email);
		employee.setPhone(phone);

		if (StringUtils.hasText(cpr)) {
			employee.setCpr(cpr);
		}
		if (StringUtils.hasText(uuid)) {
			employee.setMitIdUuid(uuid);
		}

		employee.setStartDate(startDate);
		employee.setStopDate(stopDate);
		employee.setPositionName(position);
	}

	@Transactional
	public void sendAwaitingApprovalNotifications() {
		List<Employee> awaitingApproval = findEmployeesWithPendingApprovals(List.of(ApproveStatus.AWAITING_APPROVAL, ApproveStatus.AWAITING_EXTENSION_APPROVAL));
		if (awaitingApproval.isEmpty()) {
			return;
		}

		EmailTemplate emailTemplate = emailTemplateService.getByType(EmailTemplateType.AWAITING_APPROVAL);
		if (!emailTemplate.isEnabled()) {
			return;
		}

		Map<Business, List<Employee>> employeesByBusiness = awaitingApproval.stream()
				.collect(Collectors.groupingBy(Employee::getBusiness));

		boolean anyBusinessWithoutLeaderFlow = false;

		for (Map.Entry<Business, List<Employee>> entry : employeesByBusiness.entrySet()) {
			Business business = entry.getKey();
			List<Employee> employees = entry.getValue();

			if (business.isEnableLeaders()) {
				for (Leader leader : business.getLeaders()) {
					if (!StringUtils.hasText(leader.getMail())) {
						continue;
					}

					EmailTemplateService.RenderedEmail email = emailTemplateService.render(emailTemplate, Map.of(
							EmailTemplatePlaceholder.RECIPIENT, leader.getName(),
							EmailTemplatePlaceholder.COUNT, String.valueOf(employees.size()),
							EmailTemplatePlaceholder.LINK, configuration.getBaseUrl() + "/approve"));
					messageService.sendNotificationUsingMail(leader.getMail(), email);
				}
			}
			else {
				anyBusinessWithoutLeaderFlow = true;
			}
		}

		// Businesses without an active leader flow don't get a per-leader email above,
		// so notify the general/admin inbox instead, using the total pending count across all businesses.
		if (anyBusinessWithoutLeaderFlow) {
			String generalEmail = configuration.getCustomer().getGeneralEmail();
			if (StringUtils.hasText(generalEmail)) {
				EmailTemplateService.RenderedEmail email = emailTemplateService.render(emailTemplate, Map.of(
						EmailTemplatePlaceholder.RECIPIENT, configuration.getCustomer().getName(),
						EmailTemplatePlaceholder.COUNT, String.valueOf(awaitingApproval.size()),
						EmailTemplatePlaceholder.LINK, configuration.getBaseUrl() + "/approve"));
				messageService.sendNotificationUsingMail(generalEmail, email);
			}
		}
	}
}
