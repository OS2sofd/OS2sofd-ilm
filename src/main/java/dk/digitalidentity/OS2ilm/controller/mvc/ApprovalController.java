package dk.digitalidentity.OS2ilm.controller.mvc;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dk.digitalidentity.OS2ilm.model.entity.ActivationTimeFrame;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import dk.digitalidentity.OS2ilm.model.role.RequireLeaderOrHigher;
import dk.digitalidentity.OS2ilm.model.role.RequireSystemAdministrator;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.ActivationTimeFrameService;
import dk.digitalidentity.OS2ilm.service.AdGroupService;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.OrgUnitService;
import lombok.RequiredArgsConstructor;

@Controller
@RequireLeaderOrHigher
@RequiredArgsConstructor
public class ApprovalController {
	private final EmployeeService employeeService;
	private final SecurityUtil securityUtil;
	private final OrgUnitService orgUnitService;
	private final AdGroupService adGroupService;
	private final ActivationTimeFrameService activationTimeFrameService;
	private final BusinessService businessService;

	@GetMapping("/approve")
    public String approveEmployees(Model model) {
		model.addAttribute("isAdmin", securityUtil.isAdmin());
		model.addAttribute("isLeader", securityUtil.isLeader());
        return "admin/employees/approve";
    }

	@RequireSystemAdministrator
	@GetMapping("/approval/{id}")
	public String adminApprovesEmployee(@PathVariable long id, Model model) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			return "redirect:/error";
		}
		if (employee.getApproved() == ApproveStatus.ADMIN_APPROVED) {
			return "redirect:/error";
		}
		Business business = businessService.findByIdAndLoaded(employee.getBusiness().getId());
		model.addAttribute("allOrgUnits", orgUnitService.getAllTree());
		model.addAttribute("archetypes", business.getSelectableArchetypes());
		model.addAttribute("employee", employee);
		model.addAttribute("groups", adGroupService.findAll());
		model.addAttribute("business", business);
		model.addAttribute("settings", business.getBusinessEmploymentSettings());

		model.addAttribute("isAdmin", securityUtil.isAdmin());

		List<ActivationTimeFrame> activations = activationTimeFrameService.findAll();

		model.addAttribute("activations", activationTimeFrameService.formatActivations(activations));
		return "admin/employees/adminApprove";
	}
}
