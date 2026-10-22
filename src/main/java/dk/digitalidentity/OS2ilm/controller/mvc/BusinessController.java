package dk.digitalidentity.OS2ilm.controller.mvc;

import dk.digitalidentity.OS2ilm.model.entity.ActivationTimeFrame;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Leader;
import dk.digitalidentity.OS2ilm.model.role.RequireReadOnlyAdministrator;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.role.RequireSystemAdministrator;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.ActivationTimeFrameService;
import dk.digitalidentity.OS2ilm.service.ArchetypeService;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.LeaderService;
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
	private final LeaderService leaderService;
	private final SecurityUtil securityUtil;
	private final ActivationTimeFrameService activationTimeFrameService;
	private final ArchetypeService archetypeService;

	@GetMapping("/business/overview")
    public String companies(Model model) {
		model.addAttribute("businesses", businessService.findAllAndLoaded());
		model.addAttribute("isAdmin", securityUtil.isAdmin());
		model.addAttribute("isLeader", securityUtil.isLeader());
        return "admin/business/overview";
    }

	@RequireSystemAdministrator
    @GetMapping("/business/create")
    public String createCompany(Model model) {
		List<ActivationTimeFrame> activations = activationTimeFrameService.findAll();
		model.addAttribute("activations", activationTimeFrameService.formatActivations(activations));
		model.addAttribute("archetypes", archetypeService.findAll());
        return "admin/business/createBusiness";
    }

	@RequireSystemAdministrator
    @GetMapping("/business/edit/{id}")
    public String editCompany(@PathVariable long id, Model model) {
		Business business = businessService.findByIdAndLoaded(id);
		List<ActivationTimeFrame> activations = activationTimeFrameService.findAll();
		List<Leader> leaders = leaderService.findByBusinessId(id);
		model.addAttribute("business", business);
		model.addAttribute("activations", activationTimeFrameService.formatActivations(activations));
		model.addAttribute("archetypes", archetypeService.findAll());
		model.addAttribute("leaders", leaders);

		List<EmployeeDTO> employees = new ArrayList<>();
		List<Employee> allEmployeesByBusiness = employeeService.findAllEmployeesByBusiness(business);
		for (Employee employeesByBusiness : allEmployeesByBusiness) {
			employees.add(new EmployeeDTO(employeesByBusiness.getId(), employeesByBusiness.getName(), employeesByBusiness.getEmail(), employeesByBusiness.getPhone(), (employeesByBusiness.getStartDate() != null && employeesByBusiness.getStopDate() != null) ? employeesByBusiness.getStartDate() + " - " + employeesByBusiness.getStopDate() : "Ikke angivet"));
		}
		model.addAttribute("employees", employees);

        return "admin/business/editBusiness";
    }


	public record EmployeeDTO(Long id, String name, String email, String phone, String period) {}
	@GetMapping("/business/details/{id}")
	public String details(@PathVariable long id, Model model) {
		Business business = businessService.findByIdAndLoaded(id);
		List<Employee> allEmployeesByBusiness = employeeService.findAllEmployeesByBusiness(business);
		List<EmployeeDTO> employees = new ArrayList<>();
		for (Employee employeesByBusiness : allEmployeesByBusiness) {
			employees.add(new EmployeeDTO(employeesByBusiness.getId(), employeesByBusiness.getName(), employeesByBusiness.getEmail(), employeesByBusiness.getPhone(), (employeesByBusiness.getStartDate() != null && employeesByBusiness.getStopDate() != null) ? employeesByBusiness.getStartDate() + " - " + employeesByBusiness.getStopDate() : "Ikke angivet"));
		}
		List<Leader> leaders = leaderService.findByBusinessId(id);
		model.addAttribute("business", business);
		model.addAttribute("employees", employees);
		model.addAttribute("leaders", leaders);
		model.addAttribute("activation", business.getActivationTimeFrame());
		model.addAttribute("archetypes", archetypeService.findAll());
		return "admin/business/businessDetails";
	}
}
