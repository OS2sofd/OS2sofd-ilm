package dk.digitalidentity.OS2ilm.controller.mvc;

import dk.digitalidentity.OS2ilm.model.role.NoRoleRequired;
import dk.digitalidentity.OS2ilm.model.role.RequireEmployeeAdministratorOrHigher;
import dk.digitalidentity.OS2ilm.model.role.RequireSystemAdministrator;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.ActivationTimeFrameService;
import dk.digitalidentity.OS2ilm.service.AdGroupService;
import dk.digitalidentity.OS2ilm.service.ArchetypeService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.OrgUnitService;
import dk.digitalidentity.OS2ilm.model.entity.ActivationTimeFrame;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
@NoRoleRequired
public class ApprovalController {

	private final EmployeeService employeeService;
	private final SecurityUtil securityUtil;
	private final OrgUnitService orgUnitService;
	private final ArchetypeService archetypeService;
	private final AdGroupService adGroupService;
	private final ActivationTimeFrameService activationTimeFrameService;

	@RequireEmployeeAdministratorOrHigher
	@GetMapping("/approve")
    public String approveEmployees(Model model) {
		model.addAttribute("isAdmin", securityUtil.isAdmin());
		model.addAttribute("isLeader", securityUtil.isLeader());
        return "admin/employees/approve";
    }

	@RequireSystemAdministrator
	@GetMapping("/approval/{id}")
	public String adminApprovesEmployee(@PathVariable long id, Model model) {
		model.addAttribute("allOrgUnits", orgUnitService.getAllTree());
		model.addAttribute("archetypes", archetypeService.findAll());
		model.addAttribute("employee", employeeService.findById(id));
		model.addAttribute("groups", adGroupService.findAll());

		model.addAttribute("isAdmin", securityUtil.isAdmin());

		List<ActivationTimeFrame> activations = activationTimeFrameService.findAll();

		model.addAttribute("activations", activationTimeFrameService.formatActivations(activations));
		return "admin/employees/adminApprove";
	}
}
