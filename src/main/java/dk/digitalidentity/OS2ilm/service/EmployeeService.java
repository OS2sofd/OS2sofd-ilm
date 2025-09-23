package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.dao.EmployeeDao;
import dk.digitalidentity.OS2ilm.model.entity.AdGroup;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.OrgUnit;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmployeeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {
	private final EmployeeDao employeeDao;
	private final IlmConfiguration configuration;
	private final EmployeeDeletionService employeeDeletionService;

	public List<Employee> findAll() {
		return employeeDao.findAll();
	}

	public void delete(Employee employee) {
		employeeDao.delete(employee);
	}

	public Employee save(Employee employee) {
		return employeeDao.save(employee);
	}

	public Employee findByIdWithUserRolesAndAdGroups(Long id) {
		return employeeDao.findByIdWithUserRolesAndAdGroups(id);
	}

	public Employee findById(Long id) {
		return employeeDao.findById(id).orElse(null);
	}

	public List<Employee> findAllEmployeesByBusiness(Business agency) {
		return employeeDao.findAllEmployeesByBusiness(agency);
	}

	public List<Employee> findByCvr(String cvr) {
		return employeeDao.findByCvr(cvr);
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


	public String listAdUsers(Employee employee) {
		return employee.getAdGroups().stream()
				.map(AdGroup::getName)
				.collect(Collectors.joining(", "));
	}
}
