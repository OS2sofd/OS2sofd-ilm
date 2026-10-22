package dk.digitalidentity.OS2ilm.controller.rest;

import dk.digitalidentity.OS2ilm.model.role.RequireEmployeeRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import dk.digitalidentity.OS2ilm.model.entity.EmailTemplate;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplateType;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.ADQueueService;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.MessageService;
import dk.digitalidentity.OS2ilm.service.EmailTemplateService;
import dk.digitalidentity.OS2ilm.util.RandomPasswordGeneratorUtil;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

@Hidden
@RequestMapping("/rest/profile")
@RestController
@RequireEmployeeRole
@RequiredArgsConstructor
public class ProfileRestController {
	private final EmployeeService employeeService;
	private final AuditLogService auditLogService;
	private final SecurityUtil securityUtil;
	private final IlmConfiguration ilmConfiguration;
	private final MessageService messageService;
	private final EmailTemplateService emailTemplateService;
	private final ADQueueService aDQueueService;

	public record EditProfileRecord(long id, String name, String phoneNumber){}
	record EmployeeLogRecord(Long id, String name, String email) {}
	@Transactional
	@PostMapping("/edit")
	public ResponseEntity<?> editProfile(@RequestBody EditProfileRecord editProfileRecord) {
		Optional<Employee> employee = securityUtil.getEmployee();
		if (employee.isEmpty() || employee.get().getId() != editProfileRecord.id) {
			return new ResponseEntity<>("Ingen adgang til denne medarbejder", HttpStatus.FORBIDDEN);
		}

		if (!securityUtil.isActiveEmployee()) {
			return new ResponseEntity<>("Ingen adgang", HttpStatus.FORBIDDEN);
		}

		if (!StringUtils.hasText(editProfileRecord.name())) {
			return new ResponseEntity<>("Du skal angive et navn", HttpStatus.BAD_REQUEST);
		}

		Employee existingEmployee = employee.get();

		if (!existingEmployee.getName().equals(editProfileRecord.name)) {
			existingEmployee.setName(editProfileRecord.name);
		}

		if (editProfileRecord.phoneNumber != null && (existingEmployee.getPhone() == null || !existingEmployee.getPhone().equals(editProfileRecord.phoneNumber))) {
			existingEmployee.setPhone(editProfileRecord.phoneNumber);
		}

		existingEmployee.setName(editProfileRecord.name());

		Employee saved = employeeService.save(existingEmployee);
		String identity = saved.getMitIdUuid() != null ? saved.getMitIdUuid() : saved.getCpr();
		auditLogService.log(identity, securityUtil.getName(), editProfileRecord.id, existingEmployee.getName(), LogAction.UPDATED_PROFILE, "Medarbejder har opdateret sin profil", new EmployeeLogRecord(existingEmployee.getId(), existingEmployee.getName(), existingEmployee.getEmail()));

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/resetPassword/{id}")
	public ResponseEntity<?> resetPassword(@PathVariable long id) {
		Optional<Employee> employee = securityUtil.getEmployee();
		if (employee.isEmpty() || employee.get().getId() != id) {
			return new ResponseEntity<>("Ingen adgang til denne medarbejder", HttpStatus.FORBIDDEN);
		}

		if (!securityUtil.isActiveEmployee()) {
			return new ResponseEntity<>("Ingen adgang", HttpStatus.FORBIDDEN);
		}

		Employee existingEmployee = employee.get();

		String password = RandomPasswordGeneratorUtil.generateUserFriendlyPassword(ilmConfiguration.getActiveDirectoryConfig().getPasswordLength());
		aDQueueService.addSetPasswordToQueue(id, password);
		EmailTemplate emailTemplate = emailTemplateService.getByType(EmailTemplateType.EMPLOYEE_PASSWORD_RESET);
		if (emailTemplate.isEnabled()) {
			EmailTemplateService.RenderedEmail email = emailTemplateService.render(emailTemplate, Map.of(
					EmailTemplatePlaceholder.RECIPIENT, existingEmployee.getName(),
					EmailTemplatePlaceholder.PASSWORD, password));
			messageService.sendNotification(existingEmployee, email);
		}

		String identity = existingEmployee.getMitIdUuid() != null ? existingEmployee.getMitIdUuid() : existingEmployee.getCpr();
		auditLogService.log(identity, securityUtil.getName(), id, existingEmployee.getName(), LogAction.RESET_PASSWORD, "Medarbejder har fået deres kodeord nulstillet", new EmployeeLogRecord(existingEmployee.getId(), existingEmployee.getName(), existingEmployee.getEmail()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/requestExtension/{id}")
	public ResponseEntity<?> requestExtension(@PathVariable long id) {
		Optional<Employee> employee = securityUtil.getEmployee();
		if (employee.isEmpty() || employee.get().getId() != id) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		if (!securityUtil.isActiveEmployee()) {
			return new ResponseEntity<>("Ingen adgang", HttpStatus.FORBIDDEN);
		}

		Employee existingEmployee = employee.get();
		existingEmployee.setApproved(ApproveStatus.AWAITING_EXTENSION_APPROVAL);

		Employee saved = employeeService.save(existingEmployee);
		String identity = saved.getMitIdUuid() != null ? saved.getMitIdUuid() : saved.getCpr();
		auditLogService.log(identity, securityUtil.getName(), id, existingEmployee.getName(), LogAction.REQUESTED_EXTENSION, "Medarbejder har anmodet om forlængelse", new EmployeeLogRecord(existingEmployee.getId(), existingEmployee.getName(), existingEmployee.getEmail()));

		return new ResponseEntity<>(HttpStatus.OK);
	}

}
