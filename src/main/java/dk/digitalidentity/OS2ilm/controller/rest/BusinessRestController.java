package dk.digitalidentity.OS2ilm.controller.rest;

import dk.digitalidentity.OS2ilm.model.entity.ActivationTimeFrame;
import dk.digitalidentity.OS2ilm.model.entity.Archetype;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.BusinessEmploymentSettings;
import dk.digitalidentity.OS2ilm.model.entity.OrgUnit;
import dk.digitalidentity.OS2ilm.model.entity.enums.AccountNameStrategy;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.model.exception.InvalidAccountNameException;
import dk.digitalidentity.OS2ilm.model.role.RequireSystemAdministrator;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.AccountNameTemplateService;
import dk.digitalidentity.OS2ilm.service.ActivationTimeFrameService;
import dk.digitalidentity.OS2ilm.service.ArchetypeService;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.BusinessEmploymentSettingsService;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import dk.digitalidentity.OS2ilm.service.OrgUnitService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.xml.bind.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private final ActivationTimeFrameService activationTimeFrameService;
	private final OrgUnitService orgUnitService;
	private final ArchetypeService archetypeService;
	private final BusinessEmploymentSettingsService businessEmploymentSettingsService;
	private final AccountNameTemplateService accountNameTemplateService;

	public record BusinessRecord(String name, String cvr, String prefix, long activationPeriod, String ou, boolean activeLeader, boolean inherit, Integer maxFutureDays, Integer maxEmploymentDays, Integer defaultEmploymentDays, List<Long> archetypesToRemove, List<Long> archetypesToAdd, AccountNameStrategy nameStrategy, String accountNameTemplate, boolean enableOrdering) {}
	public record BusinessLogRecord(Long id, String name, String cvr, String prefix, String ou, boolean activeLeader, boolean inherit, Integer maxFutureDays, Integer maxEmploymentDays, Integer defaultEmploymentDays, List<String> addedArchetypes, List<String> removedArchetypes) {}
	@PostMapping("/edit/{id}")
	public ResponseEntity<?> edit(@PathVariable long id, @RequestBody BusinessRecord businessRecord) throws ValidationException {
		Business business = businessService.findByIdAndLoaded(id);
		if (business == null || !StringUtils.hasText(businessRecord.name)) {
			return new ResponseEntity<>("Du mangler at angive navn", HttpStatus.BAD_REQUEST);
		}
		if (businessRecord.nameStrategy == AccountNameStrategy.PREFIXED && !StringUtils.hasText(businessRecord.prefix)) {
			return new ResponseEntity<>("Du mangler at angive et prefix", HttpStatus.BAD_REQUEST);
		}
		if (businessRecord.nameStrategy == AccountNameStrategy.TAILORED) {
			try {
				accountNameTemplateService.validateTemplateSyntax(businessRecord.accountNameTemplate);
			} catch (InvalidAccountNameException ex) {
				return new ResponseEntity<>("Ugyldigt brugernavnsformat", HttpStatus.BAD_REQUEST);
			}
		}

		String oldCvr = business.getCvr();
		if (StringUtils.hasText(businessRecord.cvr) && !businessRecord.cvr.equals(oldCvr)) {
			if (invalidCvr(businessRecord.cvr)) {
				return new ResponseEntity<>("Cvr-nummer er ugyldigt", HttpStatus.BAD_REQUEST);
			}

			Business existing = businessService.findBusinessByCvr(businessRecord.cvr);
			if (existing != null && existing.getId() != business.getId()) {
				log.warn("Cvr already exists in DB: {}", businessRecord.cvr);
				return new ResponseEntity<>("Cvr eksisterer allerede i løsningen", HttpStatus.BAD_REQUEST);
			}

			businessService.updateCvr(business, businessRecord.cvr);
		}

		if (businessRecord.activationPeriod == 0) {
			business.setActivationTimeFrame(null);
		} else {
			// We don't null check because the default is null
			ActivationTimeFrame activationTimeFrame = activationTimeFrameService.findById(businessRecord.activationPeriod);
			business.setActivationTimeFrame(activationTimeFrame);
		}

		if (StringUtils.hasText(businessRecord.ou)) {
			OrgUnit orgUnit = orgUnitService.findOrgUnitByUuid(businessRecord.ou);
			if (orgUnit != null) {
				business.setDefaultOrgUnit(orgUnit);
			}
		}

		List<String> removedArchetypes = new ArrayList<>();
		businessRecord.archetypesToRemove().forEach(bId -> {
			Archetype archetype = archetypeService.findById(bId);
			if (archetype != null) {
				removedArchetypes.add("name: " + archetype.getName() + " with id: " + bId);
				business.removeSelectableArchetype(archetype);
			}
		});

		List<String> addedArchetypes = new ArrayList<>();
		businessRecord.archetypesToAdd().forEach(bId -> {
			Archetype archetype = archetypeService.findById(bId);
			if (archetype != null) {
				addedArchetypes.add("name: " + archetype.getName() + " with id: " + bId);
				business.addSelectableArchetype(archetype);
			}
		});

		business.setInheritPrivileges(businessRecord.inherit);
		business.setEnableLeaders(businessRecord.activeLeader);
		business.setName(businessRecord.name);
		business.setPrefix(businessRecord.prefix);
		Business saved = businessService.save(business);

		if (business.getBusinessEmploymentSettings() != null) {
			BusinessEmploymentSettings settings = businessEmploymentSettingsService.setBusinessSettings(businessRecord.maxFutureDays, businessRecord.maxEmploymentDays, businessRecord.defaultEmploymentDays, businessRecord.nameStrategy, businessRecord.accountNameTemplate, businessRecord.enableOrdering, business.getBusinessEmploymentSettings());
			saved.setBusinessEmploymentSettings(settings);
			businessService.save(saved);
		} else {
			BusinessEmploymentSettings settings = businessEmploymentSettingsService.getBusinessEmploymentSettings(businessRecord.maxFutureDays, businessRecord.maxEmploymentDays, businessRecord.defaultEmploymentDays, businessRecord.nameStrategy, businessRecord.accountNameTemplate, businessRecord.enableOrdering);
			if (settings != null) {
				settings.setBusiness(saved);
				saved.setBusinessEmploymentSettings(settings);
				businessService.save(saved);
			}
		}

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), id, saved.getName(), LogAction.BUSINESS_UPDATED, "Virksomhed blev opdateret",
				new BusinessLogRecord(
						saved.getId(),
						saved.getName(),
						saved.getCvr(), saved.getPrefix(),
						saved.getDefaultOrgUnit() != null ? saved.getDefaultOrgUnit().getName() : "",
						saved.isEnableLeaders(),
						saved.isInheritPrivileges(),
						saved.getBusinessEmploymentSettings() != null ? saved.getBusinessEmploymentSettings().getMaxFutureDays() : null,
						saved.getBusinessEmploymentSettings() != null ? saved.getBusinessEmploymentSettings().getMaxEmploymentDays() : null,
						saved.getBusinessEmploymentSettings() != null ? saved.getBusinessEmploymentSettings().getDefaultEmploymentDays() : null,
						addedArchetypes,
						removedArchetypes
				)
		);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	public record CreateBusinessRecord(String name, String cvr, String prefix, long activationPeriod, String ou, boolean activeLeader, boolean inherit, Integer maxFutureDays, Integer maxEmploymentDays, Integer defaultEmploymentDays, List<Long> archetypeIds, AccountNameStrategy nameStrategy, String accountNameTemplate, boolean enableOrdering) {}
	public record CreateBusinessLogRecord(Long id, String name, String cvr, String prefix, String ou, boolean activeLeader, boolean inherit, Integer maxFutureDays, Integer maxEmploymentDays, Integer defaultEmploymentDays, List<String> archetypes) {}
	@PostMapping("/create")
	public ResponseEntity<?> create(@RequestBody CreateBusinessRecord businessRecord) throws ValidationException {
		if (!StringUtils.hasText(businessRecord.name) || !StringUtils.hasText(businessRecord.cvr)) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		if (businessRecord.nameStrategy == AccountNameStrategy.PREFIXED && !StringUtils.hasText(businessRecord.prefix)) {
			return new ResponseEntity<>("Du mangler at angive et prefix", HttpStatus.BAD_REQUEST);
		}
		if (businessRecord.nameStrategy == AccountNameStrategy.TAILORED) {
			try {
				accountNameTemplateService.validateTemplateSyntax(businessRecord.accountNameTemplate);
			} catch (InvalidAccountNameException ex) {
				return new ResponseEntity<>("Ugyldigt brugernavnsformat", HttpStatus.BAD_REQUEST);
			}
		}

		if (businessService.findBusinessByCvr(businessRecord.cvr) != null) {
			log.warn("Cvr already exists in DB: {}", businessRecord.cvr);
			return new ResponseEntity<>("Cvr eksisterer allerede i løsningen", HttpStatus.BAD_REQUEST);
		}

		Business newBusiness = new Business();

		if (businessRecord.activationPeriod == 0) {
			newBusiness.setActivationTimeFrame(null);
		} else {
			// We don't null check because the default is null
			ActivationTimeFrame activationTimeFrame = activationTimeFrameService.findById(businessRecord.activationPeriod);
			newBusiness.setActivationTimeFrame(activationTimeFrame);
		}

		if (StringUtils.hasText(businessRecord.ou)) {
			OrgUnit orgUnit = orgUnitService.findOrgUnitByUuid(businessRecord.ou);
			if (orgUnit != null) {
				newBusiness.setDefaultOrgUnit(orgUnit);
			}
		}

		List<String> addedArchetypes = new ArrayList<>();
		if (businessRecord.archetypeIds != null) {
			businessRecord.archetypeIds().forEach(id -> {
				Archetype archetype = archetypeService.findById(id);
				if (archetype != null) {
					addedArchetypes.add("name: " + archetype.getName() + " with id: " + id);
					newBusiness.addSelectableArchetype(archetype);
				}
			});
		}

		newBusiness.setInheritPrivileges(businessRecord.inherit);
		newBusiness.setEnableLeaders(businessRecord.activeLeader);
		newBusiness.setName(businessRecord.name);
		newBusiness.setCvr(businessRecord.cvr);
		newBusiness.setPrefix(businessRecord.prefix);
		Business saved = businessService.save(newBusiness);

		BusinessEmploymentSettings settings = businessEmploymentSettingsService.getBusinessEmploymentSettings(businessRecord.maxFutureDays, businessRecord.maxEmploymentDays, businessRecord.defaultEmploymentDays, businessRecord.nameStrategy, businessRecord.accountNameTemplate, businessRecord.enableOrdering);
		if (settings != null) {
			settings.setBusiness(saved);
			saved.setBusinessEmploymentSettings(settings);
			businessService.save(saved);
		}

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), saved.getId(), saved.getName(), LogAction.BUSINESS_CREATED, "Virksomhed blev oprettet",
				new CreateBusinessLogRecord(
						saved.getId(),
						saved.getName(),
						saved.getCvr(),
						saved.getPrefix(),
						saved.getDefaultOrgUnit() != null ? saved.getDefaultOrgUnit().getName() : "",
						saved.isEnableLeaders(),
						saved.isInheritPrivileges(),
						settings != null ? settings.getMaxFutureDays() : null,
						settings != null ? settings.getMaxEmploymentDays() : null,
						settings != null ? settings.getDefaultEmploymentDays() : null,
						addedArchetypes
				)
		);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	private static boolean invalidCvr(String cvr) {
		return cvr == null || cvr.length() != 8 || !cvr.chars().allMatch(Character::isDigit);
	}

	public record DeleteBusinessLogRecord(Long id, String name) {}
	@PostMapping("/delete/{id}")
	public ResponseEntity<?> delete(@PathVariable long id) {
		Business business = businessService.findById(id);
		if (business == null) {
			log.warn("Business with id {} not found", id);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		DeleteBusinessLogRecord blRecord = new DeleteBusinessLogRecord(
				business.getId(),
				business.getName()
		);

		businessService.delete(business);

		String performerId = securityUtil.getPerformerId();
		auditLogService.log(performerId, securityUtil.getName(), id, blRecord.name, LogAction.BUSINESS_DELETED, "Virksomhed blev slettet", blRecord);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping("/admin/employees/{businessId}/activation")
	public Map<String, Object> getBusinessActivation(@PathVariable Long businessId) {
		Business business = businessService.findById(businessId);
		Map<String, Object> response = new HashMap<>();

		if (business != null && business.getActivationTimeFrame() != null) {
			response.put("activationTimeFrameId", business.getActivationTimeFrame().getId());
		} else {
			response.put("activationTimeFrameId", null);
		}

		return response;
	}

	public record ArchetypeRecord(Long id, String name) {}
	@GetMapping("/{id}/archetypes")
	public ResponseEntity<?> getSelectableArchetypes(@PathVariable Long id) {
		Business business = businessService.findByIdAndLoaded(id);
		if (business == null) {
			log.warn("Business with id {} not found", id);
			return new ResponseEntity<>("Virksomheden findes ikke", HttpStatus.BAD_REQUEST);
		}

		Set<ArchetypeRecord> archetypeRecords = new HashSet<>();

		business.getSelectableArchetypes().forEach(archetype -> {
			ArchetypeRecord archetypeRecord = new ArchetypeRecord(archetype.getId(), archetype.getName());
			archetypeRecords.add(archetypeRecord);
		});

		return new ResponseEntity<>(archetypeRecords, HttpStatus.OK);
	}
}
