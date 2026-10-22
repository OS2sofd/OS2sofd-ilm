package dk.digitalidentity.OS2ilm.controller.mvc;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.role.RequireEmployeeRole;
import dk.digitalidentity.OS2ilm.model.role.RequireLeaderOrHigher;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.ActivationTimeFrameService;
import dk.digitalidentity.OS2ilm.service.AdGroupService;
import dk.digitalidentity.OS2ilm.service.ArchetypeService;
import dk.digitalidentity.OS2ilm.model.role.RequireLeader;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.OrgUnitService;
import dk.digitalidentity.OS2ilm.service.RoleGroupService;
import dk.digitalidentity.OS2ilm.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequireEmployeeRole
@RequiredArgsConstructor
public class EmployeeController {
	private final EmployeeService employeeService;
	private final OrgUnitService orgUnitService;
	private final BusinessService businessService;
	private final ArchetypeService archetypeService;
	private final AdGroupService adGroupService;
	private final SecurityUtil securityUtil;
	private final ActivationTimeFrameService activationTimeFrameService;
	private final UserRoleService userRoleService;
	private final RoleGroupService roleGroupService;
	private final IlmConfiguration ilmConfiguration;

	@RequireLeaderOrHigher
	@GetMapping("/employees/overview")
    public String employees(Model model) {
		List<String> cvrs = securityUtil.getCvrs();
		log.debug("Leader CVRs from token: {}", cvrs);
		if (cvrs.isEmpty()) {
			List<Employee> all = employeeService.findAll();
			log.debug("No CVRs (admin), showing all {} employees", all.size());
			model.addAttribute("employees", all);
		} else {
			List<Employee> filtered = employeeService.findByCvrIn(cvrs);
			log.debug("Filtered by CVRs, found {} employees", filtered.size());
			model.addAttribute("employees", filtered);
		}
		model.addAttribute("isAdmin", securityUtil.isAdmin());
		model.addAttribute("isLeader", securityUtil.isLeader());
        return "admin/employees/overview";
    }

	@RequireLeader
	@GetMapping("/employee/create")
	public String createEmployee(Model model) {
		List<Business> byCvrIn = businessService.findByCvrIn(securityUtil.getCvrs());
		if (byCvrIn.isEmpty()) {
			model.addAttribute("businesses", businessService.findAllAndLoaded());
		}
		else {
			model.addAttribute("businesses", byCvrIn);
		}
		model.addAttribute("orgUnits", orgUnitService.findAll());
		return "admin/employees/createEmployee";
	}

	@RequireLeader
	@GetMapping("/employee/admin/create")
	public String adminCreateEmployee(Model model) {
		model.addAttribute("businesses", businessService.findAllAndLoaded());
		model.addAttribute("orgUnits", orgUnitService.findAll());
		model.addAttribute("archetypes", archetypeService.findAll());
		model.addAttribute("groups", adGroupService.findAll());
		model.addAttribute("activations", activationTimeFrameService.findAll());
		return "admin/employees/adminCreateEmployee";
	}

	@RequireLeader
    @GetMapping("/employee/edit/{id}")
    public String editEmployee(Model model, @PathVariable long id) {
		Employee employee = employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(id);
		if (employee == null || securityUtil.hasNoAccessToEmployee(employee)) {
			return "redirect:/employees/overview";
		}
		model.addAttribute("employee", employee);
		model.addAttribute("isAdmin", securityUtil.isAdmin());
		List<Business> byCvrIn = businessService.findByCvrIn(securityUtil.getCvrs());
		if (byCvrIn.isEmpty()) {
			model.addAttribute("businesses", businessService.findAll());
		}
		else {
			model.addAttribute("businesses", byCvrIn);
		}
		model.addAttribute("groups", adGroupService.findAll());
		model.addAttribute("roles", userRoleService.findAll());
		model.addAttribute("roleGroups", roleGroupService.findAll());
		return "admin/employees/editEmployee";
	}

	@GetMapping("/employee/my/account/{id}")
	public String myAccount(@PathVariable long id,  Model model) {
		if (securityUtil.isFirstTimeLogin()) {
			return "redirect:/registration";
		}

		Optional<Employee> employee = securityUtil.getEmployee();
		if (employee.isEmpty() || employee.get().getId() != id) {
			return "redirect:/error";
		}
		Optional<String> redirect = securityUtil.getEmployeeRedirect(employee.get());
		if (redirect.isPresent()) {
			return redirect.get();
		}

		model.addAttribute("employee", employee.get());
		if (ilmConfiguration.isPasswordResetLinkEnabled()) {
			model.addAttribute("alternativeResetPasswordLink", ilmConfiguration.getPasswordResetLinkUrl());
		}
		return "employee/myAccount";
	}

	@RequireLeaderOrHigher
	@GetMapping("/employee/details/{id}")
	public String details(@PathVariable long id, Model model) {
		Employee employee = employeeService.findById(id);
		if (employee == null || securityUtil.hasNoAccessToEmployee(employee)) {
			return "redirect:/employees/overview";
		}
		model.addAttribute("employee", employee);
		return "admin/employees/employeeDetails";
	}
}
