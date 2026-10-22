package dk.digitalidentity.OS2ilm.model.entity.enums;

import lombok.Getter;

@Getter
public enum CmsProperty {
	LOGO("cms.logo", "Her kan du uploade et logo, som præsenteres for alle brugerne i øverste venstre hjørne. Det er vigtigt at logoet er i PNG format, og har en gennemsigtig baggrund.", CmsType.TEXT),
	BUSINESS_PREFIX_EXPLANATION("cms.business.prefix", "Tekst der fremgår ved forklaring af virksomhedsprefix for virksomheds oprettelse", CmsType.TEXT),
	TERMS_OF_SERVICE_TEXT("cms.tos.text", "Tekst der fremgår af de generelle vilkår og privatlivspolitik", CmsType.TEXT),
	ACTIVATE_LEADER_EXPLANATION("cms.activate.leader", "Tekst der fremgår ved forklaring af hvad det betyder at aktivere lederen på en virksomhed", CmsType.TEXT),
	INHERIT_PRIVILEGES_EXPLANATION("cms.inherit.privileges", "Tekst der fremgår ved forklaring af hvad det betyder at aktivere nedarvning af rettigheder på en virksomhed", CmsType.TEXT),
	MAX_DAYS_IN_FUTURE_EXPLANATION("cms.max.days.in.future", "Tekst der fremgår ved forklaring af hvad det betyder at man højst må oprette personer så langt i fremtiden som angivet", CmsType.TEXT),
	MAX_EMPLOYMENT_DAYS_EXPLANATION("cms.max.employment.days", "Tekst der fremgår ved forklaring af hvad det betyder at højst må ansættes i så langt som angivet", CmsType.TEXT),
	DEFAULT_EMPLOYMENT_DAYS_EXPLANATION("cms.default.employment.days", "Tekst der fremgår ved forklaring af hvad det betyder at man som standard er ansat i så langt som angivet", CmsType.TEXT),
	LANDING_PAGE_TEXT("cms.landingpage.text", "Velkomst- og vejledningsteksten der vises til konsulenter på landingssiden, hvor de logger ind med MitID.", CmsType.TEXT),
	LANDING_PAGE_BUTTON("cms.landingpage.button", "Teksten på login-knappen på landingssiden.", CmsType.TEXT),
	LANDING_PAGE_HELP("cms.landingpage.help", "Den lille hjælpetekst der vises under login-knappen på landingssiden.", CmsType.TEXT);

	private final String key;
	private final String description;
	private final boolean simple;
	private final CmsType cmsType;

	private CmsProperty(String key, String description, CmsType cmsType) {
		this.key = key;
		this.description = description;
		this.simple = false;
		this.cmsType = cmsType;
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
