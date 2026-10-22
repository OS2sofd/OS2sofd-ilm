package dk.digitalidentity.OS2ilm.security;

public class SecurityConstants {
	public static final String PROFESSIONAL_UUID_PERSISTANT = "https://data.gov.dk/model/core/eid/professional/uuid/persistent";
	public static final String PROFESSIONAL_CVR = "https://data.gov.dk/model/core/eid/professional/cvr";

	public static final String PERSONAL_CPR = "https://data.gov.dk/model/core/eid/cprNumber";

	public static final String USERNAME = "https://data.gov.dk/model/core/eid/fullName";
	public static final String EMAIL = "https://data.gov.dk/model/core/eid/email";

	// MitID substitutes the name with this literal when the person has name and address protection
	public static final String PSEUDONYM_NAME = "Pseudonym";

	private SecurityConstants() {
		throw new IllegalStateException();
	}
}
