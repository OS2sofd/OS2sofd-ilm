package dk.digitalidentity.OS2ilm.model.entity.enums;

import lombok.Getter;

@Getter
public enum CmsProperty {
	LOGO("cms.logo", "Her kan du uploade et logo, som præsenteres for alle brugerne i øverste venstre hjørne. Det er vigtigt at logoet er i PNG format, og har en gennemsigtig baggrund."),
	EMPLOYEE_APPROVED("cms.employee.approved", "Tekst der fremgår ved accept af en medarbejders anmodning"),
	EMPLOYEE_EXTENDED("cms.employee.extended", "Tekst der fremgår ved forlængelse af en medarbejders AD-Konti"),
	EMPLOYEE_CLOSED("cms.employee.closed", "Tekst der fremgår ved lukning af en medarbejders AD-Konti"),
	EMPLOYEE_CREATED("cms.employee.created", "Tekst der fremgår ved oprettelse af en medarbejder"),
	EMPLOYEE_PASSWORD_RESET("cms.employee.password.reset", "Tekst der fremgår ved nulstilling af en medarbejders AD-Konti kodeord"),
	BUSINESS_PREFIX_EXPLANATION("cms.business.prefix", "Tekst der fremgår ved forklaring af virksomhedsprefix for virksomheds oprettelse"),
	TERMS_OF_SERVICE_TEXT("cms.tos.text", "Tekst der fremgår af de generelle vilkår og privatlivspolitik");


	private final String key;
	private final String description;
	private final boolean simple;

	private CmsProperty(String key, String description) {
		this.key = key;
		this.description = description;
		this.simple = false;
	}

	public static CmsProperty findByKey(String key) {
		for (CmsProperty value : CmsProperty.values()) {
			if (value.getKey().equalsIgnoreCase(key)) {
				return value;
			}
		}

		return null;
	}
}
