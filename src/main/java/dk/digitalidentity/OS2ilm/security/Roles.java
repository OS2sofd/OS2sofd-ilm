package dk.digitalidentity.OS2ilm.security;

public class Roles {

	private Roles() {

	}

	// implicit role, granted when login is through MitID (privat)
	public static final String ROLE_EMPLOYEE = "ROLE_EMPLOYEE";	// Medarbejder

	// roles granted either through database assignment combined with MitID (erhverv) login, or through KOMBIT login
	public static final String ROLE_EMPLOYEE_ADMINISTRATOR = "ROLE_EMPLOYEE_ADMINISTRATOR";	// chef/leder

	// roles granted only through KOMBIT login for municipal employees only
	public static final String ROLE_SYSTEM_ADMINISTRATOR = "ROLE_SYSTEM_ADMINISTRATOR";	// Systemadministrator
	public static final String ROLE_SYSTEM_READONLY = "ROLE_SYSTEM_READONLY"; // Systemadministrator med læseadgang
}
