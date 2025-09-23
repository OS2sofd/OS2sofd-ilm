package dk.digitalidentity.OS2ilm.controller.rest;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.model.role.NoRoleRequired;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.ActiveDirectorySyncService;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.MessageService;
import dk.digitalidentity.OS2ilm.service.cms.CmsMessageBundle;
import dk.digitalidentity.OS2ilm.util.RandomPasswordGeneratorUtil;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@Slf4j
@RequestMapping("/rest/profile")
@RestController
@NoRoleRequired
@RequiredArgsConstructor
public class ProfileRestController {
	private final EmployeeService employeeService;
	private final ActiveDirectorySyncService activeDirectorySyncService;
	private final AuditLogService auditLogService;
	private final SecurityUtil securityUtil;
	private final IlmConfiguration ilmConfiguration;
	private final MessageService messageService;
	private final CmsMessageBundle cmsMessageBundle;

	public record EditProfileRecord(long id, String name, String phoneNumber){}

	record EmployeeLogRecord(String name, String email) {}

	@Transactional
	@PostMapping("/edit/profile")
	public ResponseEntity<?> editProfile(@RequestBody EditProfileRecord editProfileRecord) {
		Employee employee = employeeService.findById(editProfileRecord.id);
		if (employee == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		if (!employee.getName().equals(editProfileRecord.name)) {
			employee.setName(editProfileRecord.name);
		}
		if (editProfileRecord.phoneNumber != null) {
			if (employee.getPhone() == null || !employee.getPhone().equals(editProfileRecord.phoneNumber)) {
				employee.setPhone(editProfileRecord.phoneNumber);
			}
		}

		employee.setName(editProfileRecord.name());

		employeeService.save(employee);

		String iden = securityUtil.getEmployee().getMitIdUuid() != null ? securityUtil.getEmployee().getMitIdUuid() : securityUtil.getEmployee().getCpr();
		auditLogService.log(iden, securityUtil.getName(), editProfileRecord.id, employee.getName(), LogAction.UPDATED_PROFILE, "Medarbejder har opdateret sin profil", new EmployeeLogRecord(employee.getName(), employee.getEmail()));

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/resetPassword/{id}")
	public ResponseEntity<?> resetPassword(@PathVariable long id) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		String password = RandomPasswordGeneratorUtil.generateUserFriendlyPassword(ilmConfiguration.getActiveDirectoryConfig().getPasswordLength());
		activeDirectorySyncService.setPassword(employee, password);

		String message = cmsMessageBundle.getText("cms.employee.password.reset");
		message = message.replace("PLACEHOLDER_RECIPIENT", employee.getName());
		message = message.replace("PLACEHOLDER_PASSWORD", password);
		messageService.sendNotification(employee, "OS2ILM: Kodeord nulstillet", message);

		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), id, employee.getName(), LogAction.RESET_PASSWORD, "Medarbejder har fået deres kodeord nulstillet", new EmployeeLogRecord(employee.getName(), employee.getEmail()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/requestExtension/{id}")
	public ResponseEntity<?> requestExtension(@PathVariable long id) {
		Employee employee = employeeService.findById(id);
		if (employee == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		employee.setApproved(ApproveStatus.AWAITING_EXTENSION_APPROVAL);

		employeeService.save(employee);

		String iden = securityUtil.getEmployee().getMitIdUuid() != null ? securityUtil.getEmployee().getMitIdUuid() : securityUtil.getEmployee().getCpr();
		auditLogService.log(iden, securityUtil.getName(), id, employee.getName(), LogAction.REQUESTED_EXTENSION, "Medarbejder har anmodet om forlængelse", new EmployeeLogRecord(employee.getName(), employee.getEmail()));

		return new ResponseEntity<>(HttpStatus.OK);
	}

}
