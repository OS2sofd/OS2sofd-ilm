package dk.digitalidentity.OS2ilm.controller.mvc;

import dk.digitalidentity.OS2ilm.service.RoleGroupService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dk.digitalidentity.OS2ilm.model.entity.Archetype;
import dk.digitalidentity.OS2ilm.model.exception.ArchetypeNotFoundException;
import dk.digitalidentity.OS2ilm.model.role.RequireReadOnlyAdministrator;
import dk.digitalidentity.OS2ilm.model.role.RequireSystemAdministrator;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.AdGroupService;
import dk.digitalidentity.OS2ilm.service.ArchetypeService;
import dk.digitalidentity.OS2ilm.service.UserRoleService;
import lombok.RequiredArgsConstructor;

@Controller
@RequireReadOnlyAdministrator
@RequiredArgsConstructor
public class ArchetypeController {
	private final ArchetypeService archetypeService;
	private final UserRoleService userRoleService;
	private final AdGroupService adGroupService;
	private final SecurityUtil securityUtil;
	private final RoleGroupService roleGroupService;

	@GetMapping("/archetypes")
	public String getArchetypes(Model model) {
		model.addAttribute("isAdmin", securityUtil.isAdmin());
		model.addAttribute("isLeader", securityUtil.isLeader());
		return "admin/roles/archetypes";
	}

	@RequireSystemAdministrator
	@GetMapping("/archetype/create")
	public String createArchetype(Model model) {
		model.addAttribute("roles", userRoleService.findAll());
		model.addAttribute("groups", adGroupService.findAll());
		model.addAttribute("roleGroups", roleGroupService.findAll());
		return "admin/roles/createArchetype";
	}

	@RequireSystemAdministrator
	@GetMapping("/archetype/edit/{id}")
	public String editArchetype(Model model, @PathVariable Long id) throws ArchetypeNotFoundException {
		Archetype archetype = archetypeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(id);

		if (archetype == null) {
			throw new ArchetypeNotFoundException("Den arketype du forsøgte at tilgå findes ikke");
		}
		model.addAttribute("archetype", archetype);
		model.addAttribute("groups", adGroupService.findAll());
		model.addAttribute("roles", userRoleService.findAll());
		model.addAttribute("roleGroups", roleGroupService.findAll());
		return "admin/roles/editArchetype";
	}

}
