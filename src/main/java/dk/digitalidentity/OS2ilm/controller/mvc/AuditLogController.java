package dk.digitalidentity.OS2ilm.controller.mvc;

import dk.digitalidentity.OS2ilm.model.entity.AuditLog;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.model.role.RequireReadOnlyAdministrator;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequireReadOnlyAdministrator
@RequiredArgsConstructor
public class AuditLogController {
	private final AuditLogService auditLogService;

	@GetMapping("/admin/auditlog")
	public String auditLog(Model model) {
		model.addAttribute("auditLogs", auditLogService.findAll());

		model.addAttribute("logactions", LogAction.values());
		return "admin/auditlog/auditlog";
	}

	@GetMapping("/admin/auditlog/details/{id}")
	public String showDetails(Model model, @PathVariable Long id) {
		AuditLog auditLog = auditLogService.findByIdWithDetails(id);
		model.addAttribute("auditLog", auditLog);

		return "admin/auditlog/details";
	}

	@GetMapping("/admin/auditlog/correlated/{id}")
	public String showCorrelated(Model model, @PathVariable String id) {
		List<AuditLog> logs = auditLogService.findByCorrelationId(id);
		model.addAttribute("auditLogs", logs);

		return "admin/auditlog/correlated";
	}
}
