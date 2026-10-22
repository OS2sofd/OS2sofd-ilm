package dk.digitalidentity.OS2ilm.controller.api;

import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.exception.ADOperationException;
import dk.digitalidentity.OS2ilm.model.entity.Archetype;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.OrgUnit;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import dk.digitalidentity.OS2ilm.model.exception.NotCommittedException;
import dk.digitalidentity.OS2ilm.model.exception.handler.ApiExceptionHandler;
import dk.digitalidentity.OS2ilm.security.Roles;
import dk.digitalidentity.OS2ilm.service.ActiveDirectorySyncService;
import dk.digitalidentity.OS2ilm.service.AdUserService;
import dk.digitalidentity.OS2ilm.service.ArchetypeService;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.InternalAPIService;
import dk.digitalidentity.OS2ilm.service.OrgUnitService;
import dk.digitalidentity.OS2ilm.service.QueueHelperService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Hidden
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@RestController
public class InternalApiController {

	private final BusinessService businessService;
	private final EmployeeService employeeService;
	private final ArchetypeService archetypeService;
	private final ActiveDirectorySyncService activeDirectorySyncService;
	private final AdUserService adUserService;
	private final OrgUnitService orgUnitService;
	private final InternalAPIService internalAPIService;
	private final QueueHelperService queueHelperService;

	public record BusinessRequest(String name, String cvr, String prefix) {}
	public record EmployeeRequest(String name, String cpr, String email, long businessId, String approved, String cvr) {}
	public record ArchetypeRequest(String name) {}
	public record DataInjectionRequest(List<BusinessRequest> businesses, List<EmployeeRequest> employees, List<ArchetypeRequest> archetypes) {}
	@PostMapping("/inject/demodata")
	public ResponseEntity<?> injectDemoData(@RequestBody DataInjectionRequest requestBody) {
		if (requestBody == null) {
			log.warn("request body is empty");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		// Load businesses
		List<Business> newbusinesses = new ArrayList<>();
		for (BusinessRequest businessRequest : requestBody.businesses) {
			Business business = new Business();
			business.setName(businessRequest.name);
			business.setCvr(businessRequest.cvr);
			business.setPrefix(businessRequest.prefix);
			newbusinesses.add(business);
		}
		if (!newbusinesses.isEmpty()) {
			businessService.saveAll(newbusinesses);
		}

		// Load employees
		List<Employee> newEmployees = new ArrayList<>();
		for (EmployeeRequest employeeRequest : requestBody.employees) {
			Employee employee = new Employee();
			employee.setExtUuid(UUID.randomUUID().toString());
			employee.setExtAffiliationUuid(UUID.randomUUID().toString());
			employee.setName(employeeRequest.name);
			employee.setCpr(employeeRequest.cpr);
			employee.setEmail(employeeRequest.email);
			Business business = businessService.findById(employeeRequest.businessId);
			if (business == null) {
				log.warn("business id {} not found", employeeRequest.businessId);
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			employee.setBusiness(business);

			if ("ADMIN_APPROVED".equals(employeeRequest.approved)) {
				employee.setApproved(ApproveStatus.ADMIN_APPROVED);
			}
			else if ("LEADER_APPROVED".equals(employeeRequest.approved)) {
				employee.setApproved(ApproveStatus.LEADER_APPROVED);
			}
			else if ("AWAITING_APPROVAL".equals(employeeRequest.approved)) {
				employee.setApproved(ApproveStatus.AWAITING_APPROVAL);
			}
			employee.setCvr(employeeRequest.cvr);

			newEmployees.add(employee);
		}
		if (!newEmployees.isEmpty()) {
			employeeService.saveAll(newEmployees);

			newEmployees.forEach(employee -> {
				if (StringUtils.hasText(employee.getCpr())) {
					queueHelperService.sofdHelper(employee.getId());
				}
			});
		}

		List<Archetype> newArchetypes = new ArrayList<>();
		for (ArchetypeRequest archetypeRequest : requestBody.archetypes) {
			Archetype archetype = new Archetype();
			archetype.setName(archetypeRequest.name);
			newArchetypes.add(archetype);
		}
		if (!newArchetypes.isEmpty()) {
			archetypeService.saveAll(newArchetypes);
		}

		log.info("Successfully injected data into DB");
		return new ResponseEntity<>(HttpStatus.OK);
	}

	// Forces the user to change their password on their next login in AD
	@PostMapping("employee/password/forceChange/{id}")
	public ResponseEntity<?> forcePasswordChange(@PathVariable long id) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			return new ResponseEntity<>("Employee not found", HttpStatus.BAD_REQUEST);
		}
		try {
			activeDirectorySyncService.forcePasswordChange(employee);
		} catch (ADOperationException ex) {
			log.warn("Failed to force password change for employee {}: {}", id, ex.getMessage());
			return new ResponseEntity<>("Failed to force password change in AD: " + ex.getMessage(), HttpStatus.BAD_GATEWAY);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping("employee/all")
	public ResponseEntity<?> getAllEmployees() {
		List<Employee> employees = employeeService.findAll();
		return new ResponseEntity<>(employees, HttpStatus.OK);
	}

	@GetMapping("employee/byCvr/{cvr}")
	public ResponseEntity<?> getByCvr(@PathVariable String cvr) {
		List<Employee> employees = employeeService.findByCvr(cvr);
		return new ResponseEntity<>(employees, HttpStatus.OK);
	}

	@GetMapping("employee/byOrgUnit/{uuid}")
	public ResponseEntity<?> getByOrgUnit(@PathVariable String uuid) {
		OrgUnit orgUnit = orgUnitService.findOrgUnitByUuid(uuid);
		if (orgUnit == null) {
			return new ResponseEntity<>("OrgUnit not found", HttpStatus.BAD_REQUEST);
		}
		List<Employee> employees = employeeService.findByOrgUnit(orgUnit);
		return new ResponseEntity<>(employees, HttpStatus.OK);
	}

	@GetMapping("business/leader/{id}")
	public ResponseEntity<?> getLeaderOfBusiness(@PathVariable long id) {
		Business business = businessService.findById(id);
		if (business == null) {
			return new ResponseEntity<>("Business not found", HttpStatus.BAD_REQUEST);
		}
		List<Employee> result = employeeService.findByBusinessAndRole(business, Roles.ROLE_EMPLOYEE_ADMINISTRATOR);

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@GetMapping("business/all")
	public ResponseEntity<?> getAllBusinesses() {
		List<Business> businesses = businessService.findAll();
		return new ResponseEntity<>(businesses, HttpStatus.OK);
	}

	@GetMapping("orgUnit/all")
	public ResponseEntity<?> getAllOrgUnits() {
		List<OrgUnit> orgUnits = orgUnitService.findAll();
		return new ResponseEntity<>(orgUnits, HttpStatus.OK);
	}

	public record EditEmployeeRecord(long id, String name, String email, String phoneNumber, String mitIdUuid, String cpr, Long businessId, ApproveStatus approveStatus, String cvr, String positionName, LocalDate startDate, LocalDate stopDate, Long adUserId, boolean blocked, String orgUnitUuid, boolean dryRun) {}
	@GetMapping("employee/{id}")
	public ResponseEntity<?> getEmployee(@PathVariable long id) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			return new ResponseEntity<>("Employee not found", HttpStatus.BAD_REQUEST);
		}
		Long adUserId = employee.getUser() == null ? null : employee.getUser().getId();
		Long businessId = employee.getBusiness() == null ? null : employee.getBusiness().getId();
		String orgUnitUuid = employee.getOrgUnit() == null ? null : employee.getOrgUnit().getUuid();
		EditEmployeeRecord result = new EditEmployeeRecord(employee.getId(), employee.getName(), employee.getEmail(), employee.getPhone(), employee.getMitIdUuid(), employee.getCpr(), businessId, employee.getApproved(), employee.getCvr(), employee.getPositionName(), employee.getStartDate(), employee.getStopDate(), adUserId, employee.isBlocked(), orgUnitUuid, true);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@PostMapping("employee/{id}")
	public ResponseEntity<?> editEmployee(@PathVariable long id, @RequestBody EditEmployeeRecord editEmployeeRecord) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			return new ResponseEntity<>("Employee not found", HttpStatus.BAD_REQUEST);
		}
		try {
			String response = internalAPIService.updateEmployee(employee, editEmployeeRecord);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (NotCommittedException e) {
			return ApiExceptionHandler.handleInvalidRequestException(e);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping("poke/create/queue/{employeeId}")
	public ResponseEntity<?> pokeCreateQueue(@PathVariable long employeeId) {
		Employee employee = employeeService.findById(employeeId);
		if (employee == null) {
			return new ResponseEntity<>("Employee not found", HttpStatus.NOT_FOUND);
		}

		queueHelperService.sofdHelper(employeeId);
		return new ResponseEntity<>("Added employee to sofd create queue", HttpStatus.OK);
	}

	@PostMapping("ad/backfill-cpr")
	public ResponseEntity<?> backfillCpr() {
		List<AdUser> adUsers = adUserService.findAll();
		int updated = 0;
		int skipped = 0;
		List<String> errors = new ArrayList<>();

		for (AdUser adUser : adUsers) {
			Employee employee = adUser.getEmployee();
			if (employee == null || employee.getCpr() == null || employee.getCpr().isBlank()) {
				skipped++;
				continue;
			}

			try {
				activeDirectorySyncService.setCprAttribute(employee.getId(), employee.getCpr());
				updated++;
			} catch (Exception e) {
				log.warn("Failed to backfill CPR for AD user {}: {}", adUser.getName(), e.getMessage());
				errors.add(adUser.getName() + ": " + e.getMessage());
			}
		}

		log.info("Backfill complete: updated={}, skipped={}, errors={}", updated, skipped, errors.size());
		String result = "Backfill complete: updated=" + updated + ", skipped=" + skipped + ", errors=" + errors.size();
		if (!errors.isEmpty()) {
			result += ", failed accounts: " + String.join(", ", errors);
		}
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
}
