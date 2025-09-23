package dk.digitalidentity.OS2ilm.controller.mvc;

import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.role.RequireReadOnlyAdministrator;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.role.RequireSystemAdministrator;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequireReadOnlyAdministrator
@RequiredArgsConstructor
public class BusinessController {
	private final BusinessService businessService;
	private final EmployeeService employeeService;
	private final SecurityUtil securityUtil;

	@GetMapping("/business/overview")
    public String companies(Model model) {
		model.addAttribute("businesses", businessService.findAll());
		model.addAttribute("isAdmin", securityUtil.isAdmin());
		model.addAttribute("isLeader", securityUtil.isLeader());
        return "admin/business/overview";
    }

	@RequireSystemAdministrator
    @GetMapping("/business/create")
    public String createCompany() {
        return "admin/business/createBusiness";
    }

	@RequireSystemAdministrator
    @GetMapping("/business/edit/{id}")
    public String editCompany(@PathVariable long id, Model model) {
		model.addAttribute("business", businessService.findById(id));
        return "admin/business/editBusiness";
    }


	public record EmployeeDTO(Long id, String name, String email, String phone, String period) {}
	@GetMapping("/business/details/{id}")
	public String details(@PathVariable long id, Model model) {
		Business business = businessService.findById(id);
		model.addAttribute("business", business);
		List<Employee> allEmployeesByBusiness = employeeService.findAllEmployeesByBusiness(business);
		List<EmployeeDTO> employees = new ArrayList<>();
		for (Employee employeesByBusiness : allEmployeesByBusiness) {
			employees.add(new EmployeeDTO(employeesByBusiness.getId(), employeesByBusiness.getName(), employeesByBusiness.getEmail(), employeesByBusiness.getPhone(), (employeesByBusiness.getStartDate() != null && employeesByBusiness.getStopDate() != null) ? employeesByBusiness.getStartDate() + " - " + employeesByBusiness.getStopDate() : "Ikke angivet"));
		}
		model.addAttribute("employees", employees);
		return "admin/business/businessDetails";
	}
}
