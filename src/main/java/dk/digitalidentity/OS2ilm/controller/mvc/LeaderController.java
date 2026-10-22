package dk.digitalidentity.OS2ilm.controller.mvc;

import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Leader;
import dk.digitalidentity.OS2ilm.model.role.RequireReadOnlyAdministrator;
import dk.digitalidentity.OS2ilm.model.role.RequireSystemAdministrator;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import dk.digitalidentity.OS2ilm.service.LeaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequireSystemAdministrator
@RequiredArgsConstructor
public class LeaderController {
	private final LeaderService leaderService;
	private final BusinessService businessService;
	private final SecurityUtil securityUtil;

	@RequireReadOnlyAdministrator
	@GetMapping("/leader/overview")
	public String overview(Model model) {
		model.addAttribute("leaders", leaderService.findAllWithBusinesses());
		model.addAttribute("isAdmin", securityUtil.isAdmin());
		return "admin/leader/overview";
	}

	@GetMapping("/leader/create")
	public String create(Model model) {
		model.addAttribute("businesses", businessService.findAll());
		return "admin/leader/createLeader";
	}

	@GetMapping("/leader/edit/{id}")
	public String edit(@PathVariable long id, Model model) {
		Leader leader = leaderService.findByIdWithBusinesses(id).orElse(null);
		if (leader == null) {
			return "redirect:/leader/overview";
		}
		Set<Long> leaderBusinessIds = leader.getBusinesses().stream()
				.map(Business::getId)
				.collect(Collectors.toSet());
		model.addAttribute("leader", leader);
		model.addAttribute("leaderBusinessIds", leaderBusinessIds);
		model.addAttribute("businesses", businessService.findAll());
		return "admin/leader/editLeader";
	}
}
