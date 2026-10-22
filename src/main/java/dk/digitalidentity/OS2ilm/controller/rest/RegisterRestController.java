package dk.digitalidentity.OS2ilm.controller.rest;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import dk.digitalidentity.OS2ilm.model.entity.EmailTemplate;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplateType;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.model.role.NoRoleRequired;
import dk.digitalidentity.OS2ilm.security.Roles;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.MessageService;
import dk.digitalidentity.OS2ilm.service.QueueHelperService;
import dk.digitalidentity.OS2ilm.service.EmailTemplateService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Hidden
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/rest/register")
@NoRoleRequired
@RestController
public class RegisterRestController {

	private final BusinessService businessService;
	private final EmployeeService employeeService;
	private final SecurityUtil securityUtil;
	private final AuditLogService auditLogService;
	private final EmailTemplateService emailTemplateService;
	private final MessageService messageService;
	private final QueueHelperService queueHelperService;
	private final IlmConfiguration configuration;

	public record RegisterEmployeeRecord(String name, String mail, String leaderMail, String phone, String cvr, boolean hasCvr, String position, boolean acceptedTos, String businessName, String reason) {}
	public record RegisterEmployeeLogRecord(Long id, String name, String mail, String phone, String cvr, String position, String reason, BusinessLogRecord business) {}
	public record BusinessLogRecord(Long id, String name) {}
	@Transactional
	@PostMapping("/employee")
	public ResponseEntity<?> registerEmployee(@RequestBody RegisterEmployeeRecord registerEmployeeRecord) {
		if (registerEmployeeRecord.hasCvr() && !StringUtils.hasText(registerEmployeeRecord.cvr())) {
			log.warn("CVR is required when hasCvr is true");
			return new ResponseEntity<>("Du skal angive et CVR-nummer, fordi du har angivet at du har et.", HttpStatus.BAD_REQUEST);
		}

		Business business = businessService.findBusinessByCvr(registerEmployeeRecord.cvr);
		if (business == null) {
			log.warn("The entered CVR is not registered in the solution");
			return new ResponseEntity<>("Der findes ikke en virksomhed med det CVR-nummer du har angivet.", HttpStatus.BAD_REQUEST);
		}

		// Mirrors the lock applied in the UI: where the field is read-only the token value wins here too, so
		// the lock cannot be bypassed by posting directly. Where it is editable the posted value is used,
		// which also covers the person whose name MitID withholds.
		final String name = configuration.getCustomer().isLockNameFromMitId()
				? securityUtil.getVerifiedName().orElse(registerEmployeeRecord.name())
				: registerEmployeeRecord.name();
		if (!StringUtils.hasText(name)) {
			log.warn("Name is required for registration");
			return new ResponseEntity<>("Du skal angive et navn.", HttpStatus.BAD_REQUEST);
		}

		if (configuration.getCustomer().isRequirePhone() && !StringUtils.hasText(registerEmployeeRecord.phone())) {
			log.warn("Phone is required for registration");
			return new ResponseEntity<>("Du skal angive et telefonnummer.", HttpStatus.BAD_REQUEST);
		}

		if (configuration.getCustomer().isRequireRegistrationReason() && !StringUtils.hasText(registerEmployeeRecord.reason())) {
			log.warn("Registration reason is required for registration");
			return new ResponseEntity<>("Du skal angive en registreringsårsag.", HttpStatus.BAD_REQUEST);
		}

		// Validate cpr and mitId and check if they exist already
		final Optional<String> cpr = securityUtil.getCPRAttribute();
		final Optional<String> uuid = securityUtil.getMitIdUuid();
		if(cpr.isEmpty() && uuid.isEmpty()) {
			return new ResponseEntity<>("Kan ikke registere medarbejder, fordi der findes ingen cpr eller mitid uuid.", HttpStatus.BAD_REQUEST);
		}

		Employee existingEmployee = null;
		if (cpr.isPresent()) {
			existingEmployee = employeeService.findByCpr(cpr.get());
			if (existingEmployee != null) {
				log.warn("An employee with cpr already exists - employee id {}", existingEmployee.getId());
				return new ResponseEntity<>("Der findes allerede en bruger med dette CPR-nummer.", HttpStatus.BAD_REQUEST);
			}
		} else if (uuid.isPresent()) {
			existingEmployee = employeeService.findByMitIdUuid(uuid.get());
			if (existingEmployee != null) {
				log.warn("The employee with mitid-uuid {} already exists - employee id {}", uuid.get(), existingEmployee.getId());
				return new ResponseEntity<>("Der findes allerede en bruger med dette MitID-nummer.", HttpStatus.BAD_REQUEST);
			}
		}

		Employee employee = new Employee();

		// Set employee details
		employee.setExtUuid(UUID.randomUUID().toString());
		employee.setExtAffiliationUuid(UUID.randomUUID().toString());
		employee.setName(name);
		employee.setEmail(registerEmployeeRecord.mail);
		employee.setApproved(ApproveStatus.AWAITING_APPROVAL);
		employee.setPositionName(registerEmployeeRecord.position);
		employee.setAcceptedTos(registerEmployeeRecord.acceptedTos);
		employee.setPhone(registerEmployeeRecord.phone);
		employee.setBusiness(business);
		employee.setCvr(registerEmployeeRecord.cvr);
		employee.setRole(Roles.ROLE_EMPLOYEE);
		employee.setReason(registerEmployeeRecord.reason);

		// This check could probably be done more safely
		if (cpr.isPresent()) {
			employee.setCpr(cpr.get());
		} else if (uuid.isPresent()) {
			employee.setMitIdUuid(uuid.get());
		}

		try {
			// Save employee and update business with it
			Employee save = employeeService.save(employee);
			business.getEmployees().add(save);
			Business savedBusiness = businessService.save(business);

			EmailTemplate emailTemplate = emailTemplateService.getByType(EmailTemplateType.EMPLOYEE_CREATE_REQUEST);
			if (emailTemplate.isEnabled()) {
				EmailTemplateService.RenderedEmail email = emailTemplateService.render(emailTemplate, Map.of());
				messageService.sendNotificationUsingMail(registerEmployeeRecord.leaderMail, email);
			}

			String identity = save.getCpr() == null ? save.getMitIdUuid() : save.getCpr();
			RegisterEmployeeLogRecord audit = new RegisterEmployeeLogRecord(
					save.getId(),
					save.getName(),
					save.getEmail(),
					save.getPhone(),
					save.getCvr(),
					save.getPositionName(),
					save.getReason(),
					savedBusiness != null ? new BusinessLogRecord(savedBusiness.getId(), savedBusiness.getName()) : null
			);
			auditLogService.log(identity, securityUtil.getName(), employee.getId(), employee.getName(), LogAction.REGISTRATION, "Bruger har registreret sig i løsningen", audit);
			return new ResponseEntity<>(save.getId(), HttpStatus.OK);

		} catch (Exception e) {
			log.error("Error saving employee", e);
			return new ResponseEntity<>("Registreringen kunne ikke gennemføres. Prøv igen, eller kontakt din administrator.", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public record TosAcceptedLogRecord(Long id, String name, String email, boolean acceptedTos) {}
	@PostMapping("/tos")
	@Transactional
	public ResponseEntity<?> acceptTos() {
		final Optional<Employee> employeeResult = securityUtil.getEmployee();
		if (employeeResult.isEmpty()) {
			return new ResponseEntity<>("Ingen medarbejder fundet", HttpStatus.BAD_REQUEST);
		}
		final Employee employee = employeeResult.get();

		employee.setAcceptedTos(true);
		employeeService.save(employee);

		if (employee.getApproved() == ApproveStatus.ADMIN_APPROVED) {
			queueHelperService.sofdHelper(employee.getId());
		}

		String identity = employee.getCpr() == null ? employee.getMitIdUuid() : employee.getCpr();
		TosAcceptedLogRecord audit = new TosAcceptedLogRecord(
				employee.getId(),
				employee.getName(),
				employee.getEmail(),
				employee.isAcceptedTos()
		);
		auditLogService.log(identity, securityUtil.getName(), employee.getId(), employee.getName(), LogAction.REGISTRATION, "Bruger har registreret sig i løsningen", audit);
		return new ResponseEntity<>(employee.getId(), HttpStatus.OK);
	}
}
