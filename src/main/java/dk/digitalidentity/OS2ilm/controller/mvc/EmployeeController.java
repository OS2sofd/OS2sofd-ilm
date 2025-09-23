package dk.digitalidentity.OS2ilm.controller.mvc;

import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.role.RequireEmployeeAdministratorOrHigher;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.ActivationTimeFrameService;
import dk.digitalidentity.OS2ilm.service.AdGroupService;
import dk.digitalidentity.OS2ilm.service.ArchetypeService;
import dk.digitalidentity.OS2ilm.model.role.NoRoleRequired;
import dk.digitalidentity.OS2ilm.model.role.RequireEmployeeAdministrator;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.OrgUnitService;
import dk.digitalidentity.OS2ilm.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@NoRoleRequired
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

	@RequireEmployeeAdministratorOrHigher
	@GetMapping("/employees/overview")
    public String employees(Model model) {
		model.addAttribute("employees", employeeService.findAll());
		model.addAttribute("isAdmin", securityUtil.isAdmin());
		model.addAttribute("isLeader", securityUtil.isLeader());
        return "admin/employees/overview";
    }

	@RequireEmployeeAdministrator
	@GetMapping("/employee/create")
	public String createEmployee(Model model) {
		List<Business> byCvrIn = businessService.findByCvrIn(securityUtil.getCvrs());
		if (byCvrIn.isEmpty()) {
			model.addAttribute("businesses", businessService.findAll());
		}
		else {
			model.addAttribute("businesses", byCvrIn);
		}
		model.addAttribute("orgUnits", orgUnitService.findAll());
		return "admin/employees/createEmployee";
	}

	@RequireEmployeeAdministrator
	@GetMapping("/employee/admin/create")
	public String adminCreateEmployee(Model model) {
		model.addAttribute("businesses", businessService.findAll());
		model.addAttribute("orgUnits", orgUnitService.findAll());
		model.addAttribute("archetypes", archetypeService.findAll());
		model.addAttribute("groups", adGroupService.findAll());
		model.addAttribute("activations", activationTimeFrameService.findAll());
		return "admin/employees/adminCreateEmployee";
	}

	@RequireEmployeeAdministrator
    @GetMapping("/employee/edit/{id}")
    public String editEmployee(Model model, @PathVariable long id) {
		Employee employee = employeeService.findByIdWithUserRolesAndAdGroups(id);
		model.addAttribute("employee", employee);
		List<Business> byCvrIn = businessService.findByCvrIn(securityUtil.getCvrs());
		if (byCvrIn.isEmpty()) {
			model.addAttribute("businesses", businessService.findAll());
		}
		else {
			model.addAttribute("businesses", byCvrIn);
		}
		model.addAttribute("allGroups", adGroupService.findAdGroupsNotLinkedToEmployee(employee.getId()));
		model.addAttribute("groups", employee.getAdGroups());
		model.addAttribute("allRoles", userRoleService.findUserRolesNotLinkedToEmployee(employee.getId()));
		model.addAttribute("roles", employee.getUserRoles());

		return "admin/employees/editEmployee";
	}

	@NoRoleRequired
	@GetMapping("/employee/my/account/{id}")
	public String myAccount(@PathVariable long id,  Model model) {
		if (securityUtil.isFirstTimeLogin()) {
			return "redirect:/registration";
		}
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			return "redirect:/error";
		}
		model.addAttribute("employee", employee);
		return "employee/myAccount";
	}

	@RequireEmployeeAdministratorOrHigher
	@GetMapping("/employee/details/{id}")
	public String details(@PathVariable long id, Model model) {
		model.addAttribute("employee", employeeService.findById(id));
		return "admin/employees/employeeDetails";
	}
}
