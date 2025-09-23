package dk.digitalidentity.OS2ilm.controller.rest;

import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.model.role.NoRoleRequired;
import dk.digitalidentity.OS2ilm.security.Roles;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.ActiveDirectorySyncService;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.MessageService;
import dk.digitalidentity.OS2ilm.service.SofdService;
import dk.digitalidentity.OS2ilm.service.cms.CmsMessageBundle;
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
	private final ActiveDirectorySyncService activeDirectorySyncService;
	private final SofdService sofdService;
	private final CmsMessageBundle cmsMessageBundle;
	private final MessageService messageService;

	public record RegisterEmployeeRecord(String name, String mail, String leaderMail, String phone, String cvr, boolean hasCvr, String position, boolean acceptedTos, String businessName) {}
	@Transactional
	@PostMapping("/employee")
	public ResponseEntity<?> registerEmployee(@RequestBody RegisterEmployeeRecord registerEmployeeRecord) {
		if (registerEmployeeRecord.hasCvr()) {
			if (!StringUtils.hasText(registerEmployeeRecord.cvr())) {
				log.warn("CVR is required when hasCvr is true");
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}

		Employee employee = new Employee();
		Business business = null;
		boolean createBusiness = false;

		// Handle the business logic based cvr status
		if (registerEmployeeRecord.hasCvr) {
			business = businessService.findBusinessByCvr(registerEmployeeRecord.cvr);
			if (business == null) {
				// Business doesn't exist so we create a new one
				if (StringUtils.hasText(registerEmployeeRecord.businessName)) {
					business = new Business();
					business.setCvr(registerEmployeeRecord.cvr);
					business.setName(registerEmployeeRecord.businessName);
					business = businessService.save(business);
					createBusiness = true;
				} else {
					log.warn("Could not find business with cvr {} and no business name was provided", registerEmployeeRecord.cvr);
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
				}
			}
			employee.setBusiness(business);
			employee.setCvr(registerEmployeeRecord.cvr);
		} else {
			// No cvr case
			employee.setCvr(null);
			employee.setBusiness(null);
		}

		// Set employee details
		employee.setExtUuid(UUID.randomUUID().toString());
		employee.setName(registerEmployeeRecord.name);
		employee.setEmail(registerEmployeeRecord.mail);
		employee.setApproved(ApproveStatus.AWAITING_APPROVAL);
		employee.setPositionName(registerEmployeeRecord.position);
		employee.setAcceptedTos(registerEmployeeRecord.acceptedTos);
		employee.setPhone(registerEmployeeRecord.phone);

		String loginType = (String) securityUtil.getServletRequest().getSession().getAttribute("type");

		if (loginType.equals("leader")) {
			employee.setRole(Roles.ROLE_EMPLOYEE_ADMINISTRATOR);
		} else {
			employee.setRole(Roles.ROLE_EMPLOYEE);
		}

		// This check could probably be done more safely
		if (securityUtil.getCPRAttribute() != null) {
			employee.setCpr(securityUtil.getCPRAttribute());
		} else {
			employee.setMitIdUuid(securityUtil.getMitIdUuid());
		}

		try {
			Employee save = employeeService.save(employee);

			// Update business after adding the employee to it
			if (business != null) {
				business.getEmployees().add(save);
				businessService.save(business);
			}

			if (createBusiness) {
				auditLogService.log(employee.getCpr() == null ? employee.getMitIdUuid() : employee.getCpr(), employee.getName(), business.getId(), business.getName(), LogAction.BUSINESS_REGISTRATION, "Bruger har registreret en ny virksomhed, da deres ikke var kendt i systemet endnu.", business);
			}

			String message = cmsMessageBundle.getText("cms.employee.create.request");
			messageService.sendNotificationUsingMail(registerEmployeeRecord.leaderMail, "OS2ILM: Anmodning om godkendelse", message);

			auditLogService.log(employee.getCpr() == null ? employee.getMitIdUuid() : employee.getCpr(), null, employee.getId(), employee.getName(), LogAction.REGISTRATION, "Bruger har registreret sig i løsningen", business);
			return new ResponseEntity<>(save.getId(), HttpStatus.OK);

		} catch (Exception e) {
			log.error("Error saving employee: " + e.getMessage());
			return new ResponseEntity<>(HttpStatus.OK);
		}
	}

	@PostMapping("/tos")
	@Transactional
	public ResponseEntity<?> acceptTos() {
		Employee employee = securityUtil.getEmployee();
		if (employee == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		employee.setAcceptedTos(true);

		employeeService.save(employee);

		if (employee.getApproved() == ApproveStatus.ADMIN_APPROVED) {
			Employee updated = activeDirectorySyncService.createAdUser(employee, employee.getActivationTimeFrame().getId());
			if (updated.getAdGroups() != null) {
				updated.getAdGroups().forEach(adGroup -> activeDirectorySyncService.setGroupMembership(updated, adGroup));
			}
		}

		if (StringUtils.hasText(employee.getCpr())) {
			sofdService.sofdCreateAndUpdateHelper(employee);
		}

		return new ResponseEntity<>(employee.getId(), HttpStatus.OK);
	}
}
