package dk.digitalidentity.OS2ilm.controller.rest;

import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Leader;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.model.role.RequireSystemAdministrator;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import dk.digitalidentity.OS2ilm.service.LeaderService;
import dk.digitalidentity.OS2ilm.util.CprUtil;
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

import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Hidden
@Slf4j
@RequestMapping("/rest/leader")
@RestController
@RequiredArgsConstructor
@RequireSystemAdministrator
public class LeaderRestController {
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
	private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	private final LeaderService leaderService;
	private final BusinessService businessService;
	private final AuditLogService auditLogService;
	private final SecurityUtil securityUtil;

	public record CreateLeaderRecord(String name, String mail, String mitIdUuid, String cpr, String internalUuid, List<Long> businessIds) {}
	public record LeaderLogRecord(Long id, String name, String mail, String identifier, List<String> businesses) {}
	public record CreateLeaderResponse(String warning) {}

	@PostMapping("/create")
	public ResponseEntity<?> create(@RequestBody CreateLeaderRecord record) {
		if (!StringUtils.hasText(record.name)) {
			return new ResponseEntity<>("Navn er påkrævet", HttpStatus.BAD_REQUEST);
		}

		if (!hasIdentifier(record)) {
			return new ResponseEntity<>("Mindst én login-oplysning er påkrævet (MitID UUID, CPR eller Kommune ID)", HttpStatus.BAD_REQUEST);
		}

		if (record.businessIds == null || record.businessIds.isEmpty()) {
			return new ResponseEntity<>("Mindst én virksomhed er påkrævet", HttpStatus.BAD_REQUEST);
		}

		String normalizedCpr = CprUtil.normalize(record.cpr);
		if (StringUtils.hasText(record.cpr) && !CprUtil.isValid(record.cpr)) {
			return new ResponseEntity<>("CPR skal være 10 tegn (f.eks. 0101901234 eller 010190-1234)", HttpStatus.BAD_REQUEST);
		}

		if (StringUtils.hasText(record.mail) && !EMAIL_PATTERN.matcher(record.mail).matches()) {
			return new ResponseEntity<>("Ugyldig email-adresse", HttpStatus.BAD_REQUEST);
		}

		if (StringUtils.hasText(record.mitIdUuid) && !UUID_PATTERN.matcher(record.mitIdUuid).matches()) {
			return new ResponseEntity<>("Ugyldigt MitID UUID format", HttpStatus.BAD_REQUEST);
		}

		if (identifierExists(record, null, normalizedCpr)) {
			return new ResponseEntity<>("En leder med denne login-oplysning eksisterer allerede", HttpStatus.BAD_REQUEST);
		}

		Leader leader = new Leader();
		leader.setName(record.name);
		leader.setMail(record.mail);
		leader.setMitIdUuid(StringUtils.hasText(record.mitIdUuid) ? record.mitIdUuid : null);
		leader.setCpr(StringUtils.hasText(normalizedCpr) ? normalizedCpr : null);
		leader.setInternalUuid(StringUtils.hasText(record.internalUuid) ? record.internalUuid : null);

		Set<Business> businesses = new HashSet<>();
		List<Long> invalidBusinessIds = new ArrayList<>();
		for (Long businessId : record.businessIds) {
			Business business = businessService.findById(businessId);
			if (business != null) {
				businesses.add(business);
			} else {
				invalidBusinessIds.add(businessId);
			}
		}

		if (businesses.isEmpty()) {
			return new ResponseEntity<>("Ingen af de angivne virksomheder blev fundet", HttpStatus.BAD_REQUEST);
		}

		leader.setBusinesses(businesses);

		Leader saved;
		try {
			saved = leaderService.save(leader);
		} catch (DataIntegrityViolationException e) {
			return new ResponseEntity<>("En leder med denne login-oplysning eksisterer allerede", HttpStatus.BAD_REQUEST);
		}

		List<String> businessNames = saved.getBusinesses().stream().map(Business::getName).toList();
		String performerId = securityUtil.getPerformerId();
		auditLogService.log(
				performerId,
				securityUtil.getName(),
				saved.getId(),
				saved.getName(),
				LogAction.LEADER_CREATED,
				"Leder blev oprettet",
				new LeaderLogRecord(saved.getId(), saved.getName(), saved.getMail(), saved.getIdentifier(), businessNames)
		);

		if (!invalidBusinessIds.isEmpty()) {
			String warning = "Leder oprettet, men følgende virksomheds-ID'er blev ikke fundet: " + invalidBusinessIds;
			return new ResponseEntity<>(new CreateLeaderResponse(warning), HttpStatus.CREATED);
		}

		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PostMapping("/edit/{id}")
	public ResponseEntity<?> edit(@PathVariable long id, @RequestBody CreateLeaderRecord record) {
		Leader leader = leaderService.findByIdWithBusinesses(id).orElse(null);
		if (leader == null) {
			return new ResponseEntity<>("Leder ikke fundet", HttpStatus.NOT_FOUND);
		}

		if (!StringUtils.hasText(record.name)) {
			return new ResponseEntity<>("Navn er påkrævet", HttpStatus.BAD_REQUEST);
		}

		if (!hasIdentifier(record)) {
			return new ResponseEntity<>("Mindst én login-oplysning er påkrævet (MitID UUID, CPR eller Kommune ID)", HttpStatus.BAD_REQUEST);
		}

		if (record.businessIds == null || record.businessIds.isEmpty()) {
			return new ResponseEntity<>("Mindst én virksomhed er påkrævet", HttpStatus.BAD_REQUEST);
		}

		String normalizedCpr = CprUtil.normalize(record.cpr);
		if (StringUtils.hasText(record.cpr) && !CprUtil.isValid(record.cpr)) {
			return new ResponseEntity<>("CPR skal være 10 tegn (f.eks. 0101901234 eller 010190-1234)", HttpStatus.BAD_REQUEST);
		}

		if (StringUtils.hasText(record.mail) && !EMAIL_PATTERN.matcher(record.mail).matches()) {
			return new ResponseEntity<>("Ugyldig email-adresse", HttpStatus.BAD_REQUEST);
		}

		if (StringUtils.hasText(record.mitIdUuid) && !UUID_PATTERN.matcher(record.mitIdUuid).matches()) {
			return new ResponseEntity<>("Ugyldigt MitID UUID format", HttpStatus.BAD_REQUEST);
		}

		if (identifierExists(record, id, normalizedCpr)) {
			return new ResponseEntity<>("En anden leder med denne login-oplysning eksisterer allerede", HttpStatus.BAD_REQUEST);
		}

		leader.setName(record.name);
		leader.setMail(record.mail);
		leader.setMitIdUuid(StringUtils.hasText(record.mitIdUuid) ? record.mitIdUuid : null);
		leader.setCpr(StringUtils.hasText(normalizedCpr) ? normalizedCpr : null);
		leader.setInternalUuid(StringUtils.hasText(record.internalUuid) ? record.internalUuid : null);

		Set<Business> businesses = new HashSet<>();
		List<Long> invalidBusinessIds = new ArrayList<>();
		for (Long businessId : record.businessIds) {
			Business business = businessService.findById(businessId);
			if (business != null) {
				businesses.add(business);
			} else {
				invalidBusinessIds.add(businessId);
			}
		}

		if (businesses.isEmpty()) {
			return new ResponseEntity<>("Ingen af de angivne virksomheder blev fundet", HttpStatus.BAD_REQUEST);
		}

		leader.setBusinesses(businesses);

		Leader saved;
		try {
			saved = leaderService.save(leader);
		} catch (DataIntegrityViolationException e) {
			return new ResponseEntity<>("En anden leder med denne login-oplysning eksisterer allerede", HttpStatus.BAD_REQUEST);
		}

		List<String> businessNames = saved.getBusinesses().stream().map(Business::getName).toList();
		String performerId = securityUtil.getPerformerId();
		auditLogService.log(
				performerId,
				securityUtil.getName(),
				saved.getId(),
				saved.getName(),
				LogAction.LEADER_UPDATED,
				"Leder blev opdateret",
				new LeaderLogRecord(saved.getId(), saved.getName(), saved.getMail(), saved.getIdentifier(), businessNames)
		);

		if (!invalidBusinessIds.isEmpty()) {
			String warning = "Leder opdateret, men følgende virksomheds-ID'er blev ikke fundet: " + invalidBusinessIds;
			return new ResponseEntity<>(new CreateLeaderResponse(warning), HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/delete/{id}")
	public ResponseEntity<?> delete(@PathVariable long id) {
		Leader leader = leaderService.findById(id).orElse(null);
		if (leader == null) {
			return new ResponseEntity<>("Leder ikke fundet", HttpStatus.NOT_FOUND);
		}

		String leaderName = leader.getName();
		String identifier = leader.getIdentifier();

		leaderService.delete(leader);

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(
				performerId,
				securityUtil.getName(),
				id,
				leaderName,
				LogAction.LEADER_DELETED,
				"Leder blev slettet",
				new LeaderLogRecord(id, leaderName, null, identifier, null)
		);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private boolean hasIdentifier(CreateLeaderRecord record) {
		return StringUtils.hasText(record.mitIdUuid) || StringUtils.hasText(record.cpr) || StringUtils.hasText(record.internalUuid);
	}

	private boolean identifierExists(CreateLeaderRecord record, Long excludeId, String normalizedCpr) {
		if (StringUtils.hasText(record.mitIdUuid)) {
			Leader existing = leaderService.findByMitIdUuid(record.mitIdUuid);
			if (existing != null && (excludeId == null || existing.getId() != excludeId)) {
				return true;
			}
		}
		if (StringUtils.hasText(normalizedCpr)) {
			Leader existing = leaderService.findByCpr(normalizedCpr);
			if (existing != null && (excludeId == null || existing.getId() != excludeId)) {
				return true;
			}
		}
		if (StringUtils.hasText(record.internalUuid)) {
			Leader existing = leaderService.findByInternalUuid(record.internalUuid);
			if (existing != null && (excludeId == null || existing.getId() != excludeId)) {
				return true;
			}
		}
		return false;
	}
}
