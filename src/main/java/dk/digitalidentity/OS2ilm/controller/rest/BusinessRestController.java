package dk.digitalidentity.OS2ilm.controller.rest;

import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.model.role.RequireSystemAdministrator;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@Slf4j
@RequestMapping("/rest/business")
@RestController
@RequiredArgsConstructor
@RequireSystemAdministrator
public class BusinessRestController {
	private final BusinessService businessService;
	private final AuditLogService auditLogService;
	private final SecurityUtil securityUtil;

	record BusinessLogRecord(String name, String cvr, String prefix) {}

	public record BusinessRecord(String name, String prefix) {}
	@PostMapping("/edit/{id}")
	public ResponseEntity<?> edit(@PathVariable long id, @RequestBody BusinessRecord businessRecord) {
		Business business = businessService.findById(id);
		if (business == null || (!StringUtils.hasText(businessRecord.name) || !StringUtils.hasText(businessRecord.prefix))) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		business.setName(businessRecord.name);
		business.setPrefix(businessRecord.prefix);
		businessService.save(business);

		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), id, business.getName(), LogAction.BUSINESS_UPDATED, "Virksomhed blev opdateret", new BusinessLogRecord(business.getName(), business.getCvr(), business.getPrefix()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	public record CreateBusinessRecord(String name, String cvr, String prefix) {}
	@PostMapping("/create")
	public ResponseEntity<?> create(@RequestBody CreateBusinessRecord businessRecord) {
		if (!StringUtils.hasText(businessRecord.name) || !StringUtils.hasText(businessRecord.cvr) || !StringUtils.hasText(businessRecord.prefix)) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		if (businessService.findBusinessByCvr(businessRecord.cvr) != null) {
			log.warn("Cvr already exists in DB: {}", businessRecord.cvr);
			return new ResponseEntity<>("Cvr eksisterer allerede i løsningen", HttpStatus.BAD_REQUEST);
		}

		Business newBusiness = new Business();
		newBusiness.setName(businessRecord.name);
		newBusiness.setCvr(businessRecord.cvr);
		newBusiness.setPrefix(businessRecord.prefix);

		Business save = businessService.save(newBusiness);

		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), save.getId(), save.getName(), LogAction.BUSINESS_CREATED, "Virksomhed blev oprettet", new BusinessLogRecord(save.getName(), save.getCvr(), save.getPrefix()));
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PostMapping("/delete/{id}")
	public ResponseEntity<?> delete(@PathVariable long id) {
		Business business = businessService.findById(id);
		if (business == null) {
			log.warn("Business with id {} not found", id);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		BusinessLogRecord blRecord = new BusinessLogRecord(business.getName(), business.getCvr(), business.getPrefix());

		businessService.delete(business);

		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), id, blRecord.name, LogAction.BUSINESS_DELETED, "Virksomhed blev slettet", blRecord);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
