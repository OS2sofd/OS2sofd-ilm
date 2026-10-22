package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.dao.BusinessEmploymentSettingsDao;
import dk.digitalidentity.OS2ilm.model.entity.BusinessEmploymentSettings;
import dk.digitalidentity.OS2ilm.model.entity.enums.AccountNameStrategy;
import jakarta.xml.bind.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessEmploymentSettingsService {
	private final BusinessEmploymentSettingsDao businessEmploymentSettingsDao;
	private final AccountNameTemplateService accountNameTemplateService;

	public BusinessEmploymentSettings save(BusinessEmploymentSettings businessEmploymentSettings) {
		return businessEmploymentSettingsDao.save(businessEmploymentSettings);
	}

	public void validateBusinessSettings(Integer maxFutureDays, Integer maxEmploymentDays, Integer defaultEmploymentDays) throws ValidationException {
		if (maxFutureDays != null && maxFutureDays <= 0) {
			throw new ValidationException("Max future days must be positive");
		}
		if (maxEmploymentDays != null && maxEmploymentDays <= 0) {
			throw new ValidationException("Max employment days must be positive");
		}
		if (defaultEmploymentDays != null && maxEmploymentDays != null && defaultEmploymentDays > maxEmploymentDays) {
			throw new ValidationException("Default employment cannot exceed max employment days");
		}
	}

	public BusinessEmploymentSettings getBusinessEmploymentSettings(Integer maxFutureDays, Integer maxEmploymentDays, Integer defaultEmploymentDays, AccountNameStrategy nameStrategy, String accountNameTemplate, boolean enableOrdering) throws ValidationException {
		BusinessEmploymentSettings settings = new BusinessEmploymentSettings();
		setBusinessSettings(maxFutureDays, maxEmploymentDays, defaultEmploymentDays, nameStrategy, accountNameTemplate, enableOrdering, settings);
		return settings;
	}

	public BusinessEmploymentSettings setBusinessSettings(Integer maxFutureDays, Integer maxEmploymentDays, Integer defaultEmploymentDays, AccountNameStrategy nameStrategy, String accountNameTemplate, boolean enableOrdering, BusinessEmploymentSettings settings) throws ValidationException {
		validateBusinessSettings(maxFutureDays, maxEmploymentDays, defaultEmploymentDays);

		settings.setMaxFutureDays(maxFutureDays);
		settings.setMaxEmploymentDays(maxEmploymentDays);
		settings.setDefaultEmploymentDays(defaultEmploymentDays);
		settings.setNameStrategy(nameStrategy);
		settings.setAccountNameTemplate(accountNameTemplate);
		settings.setSofdGeneratedName(nameStrategy == AccountNameStrategy.SOFD);
		settings.setEnableOrdering(enableOrdering);

		return settings;
	}
}
