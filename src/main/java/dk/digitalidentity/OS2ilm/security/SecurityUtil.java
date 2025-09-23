package dk.digitalidentity.OS2ilm.security;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.exception.SecurityUtilRuntimeException;
import dk.digitalidentity.OS2ilm.model.entity.Admin;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.service.AdminService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.TokenUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority.Constraint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityUtil {
	public static final String ROLE_PREFIX = "ROLE_";
	private final IlmConfiguration configuration;
	private final EmployeeService employeeService;
	private final AdminService adminService;

	/**
	 * returns true if a user is currently logged in
	 */
	public boolean isAuthenticated() {
		return SecurityContextHolder.getContext().getAuthentication() != null &&
				SecurityContextHolder.getContext().getAuthentication().getDetails() != null &&
				SecurityContextHolder.getContext().getAuthentication().getDetails() instanceof TokenUser;
	}

	public boolean notAuthenticated() {
		return SecurityContextHolder.getContext().getAuthentication() == null ||
				SecurityContextHolder.getContext().getAuthentication().getDetails() == null ||
				!(SecurityContextHolder.getContext().getAuthentication().getDetails() instanceof TokenUser);
	}

	/**
	 * returns the {@link TokenUser} stored on the session for the currently logged-in user
	 */
	public TokenUser getTokenUser() {
		if (notAuthenticated()) {
			return null;
		}

		return (TokenUser) SecurityContextHolder.getContext().getAuthentication().getDetails();
	}

	/**
	 * Returns true if the current TokenUser has the supplied role
	 */
	public boolean hasRole(String role) {
		if (!isAuthenticated()) {
			return false;
		}

		return getTokenUser().getAuthorities().stream().anyMatch(ga -> Objects.equals(ga.getAuthority().toUpperCase(), role));
	}

	public Admin getAdmin() {
		String uuid = getTokenUser().getUsername();

		return adminService.findByUuid(uuid);
	}

	/**
	 * returns true if the currently logged in person is a System Admin
	 */
	public boolean isAdmin() {
		return hasRole(Roles.ROLE_SYSTEM_ADMINISTRATOR);
	}

	public boolean isReadOnlyAdmin() {
		return hasRole(Roles.ROLE_SYSTEM_READONLY);
	}

	public boolean isLeader() {
		return hasRole(Roles.ROLE_EMPLOYEE_ADMINISTRATOR);
	}

	public String getName() {
		TokenUser tokenUser = getTokenUser();
		if (tokenUser == null) {
			return null;
		}

		if (tokenUser.getAttributes() != null && !tokenUser.getAttributes().isEmpty()) {
			String name = (String) tokenUser.getAttributes().get("name");
			if (org.springframework.util.StringUtils.hasText(name)) {
				return name;
			}

			return (String) tokenUser.getAttributes().get("https://data.gov.dk/model/core/eid/fullName");
		}

		return null;
	}

	/**
	 * returns the ID (Principal) of the currently logged-in user
	 */
	public String getLoggedInPersonIdentifier() {
		if (notAuthenticated()) {
			return null;
		}

		Object object = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (object instanceof Saml2AuthenticatedPrincipal samlPrincipal) {
			return samlPrincipal.getName();
		}

		return null;
	}

	public String getCvrCSV() {
		TokenUser tokenUser = getTokenUser();
		if (tokenUser == null) {
			return null;
		}

		if (isAdmin()) {
			return null;
		}

		String result = "";
		for (SamlGrantedAuthority authority : tokenUser.getAuthorities()) {
			for (SamlGrantedAuthority.Constraint constraint : authority.getConstraints()) {
				if (constraint.getConstraintType().equals(configuration.getIdps().getCvrConstraint())) {
					result = constraint.getConstraintValue();
				}
			}
		}
		if (!org.springframework.util.StringUtils.hasText(result)) {
			if (log.isDebugEnabled()) {
				log.debug("No cvrs found for user {}", tokenUser.getUsername());
			}
			return null;
		}
		return result;
	}

	public List<String> getCvrs() {
		String cvrCSV = getCvrCSV();
		ArrayList<String> collect = new ArrayList<>();
		if (cvrCSV != null) {
			collect = Arrays.stream(cvrCSV.replace(" ", "").split(",")).collect(Collectors.toCollection(ArrayList::new));
		}
		if (log.isDebugEnabled()) {
			log.debug("Constraints: " + collect);
		}
		return collect;
	}

	// Checks whether a employee needs to register first
	public boolean isFirstTimeLogin() {
		Employee employee = getEmployee();
		return employee == null;
	}

	public boolean isTosAccepted() {
		Employee employee = getEmployee();
		return employee.isAcceptedTos();
	}

	public Employee getEmployee() {
		Employee byCpr = employeeService.findByCpr(getCPRAttribute());
		Employee byMitIdUuid = employeeService.findByMitIdUuid(getMitIdUuid());
		if (byCpr != null) {
			return byCpr;
		}
		return byMitIdUuid;
	}

	public String getCPRAttribute() {
		TokenUser tokenUser = getTokenUser();
		if (tokenUser == null) {
			throw new SecurityUtilRuntimeException("Cant fetch CPR - Not logged in!");
		}

		Object cprObj = tokenUser.getAttributes().get("https://data.gov.dk/model/core/eid/cprNumber");
		if (cprObj instanceof String cpr) {
			return cpr;
		}
		else {
			return null;
		}
	}

	public String getMitIdUuid() {
		TokenUser tokenUser = getTokenUser();
		if (tokenUser == null) {
			throw new SecurityUtilRuntimeException("Cant fetch MitIdUuid - Not logged in!");
		}

		Object mitIdUuidObj = tokenUser.getAttributes().get("https://data.gov.dk/model/core/eid/professional/uuid/persistent");
		if (mitIdUuidObj instanceof String uuid) {
			return uuid;
		}
		else {
			return null;
		}
	}

	public HttpServletRequest getServletRequest() {
		RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
		if (attribs instanceof ServletRequestAttributes) {
			return ((ServletRequestAttributes) attribs).getRequest();
		}
		return null;
	}

	// Grant roles based on how SAML sends them to us
	public void updateTokenUser(TokenUser tokenUser) {
		List<SamlGrantedAuthority> authorities = new ArrayList<>();

		if (Objects.equals(tokenUser.getIssuer(), configuration.getIdps().getEmployeeIdPEntityId())) {
			handleInternalLogin(tokenUser, authorities);
		}
		else if (Objects.equals(tokenUser.getIssuer(), configuration.getIdps().getMitIdIdPEntityId())) {
			handleMitIdLogin(tokenUser, authorities);
		}
		else {
			throw new RuntimeException("Ukendt udsteder af adgangsbillet: " + tokenUser.getIssuer());
		}
		if (authorities.isEmpty()) {
			throw new RuntimeException("Ingen roller tildelt");
		}
		tokenUser.setAuthorities(authorities);
	}

	private void handleInternalLogin(TokenUser tokenUser, List<SamlGrantedAuthority> authorities) {
		final String employeeRoleSystemAdministrator = ROLE_PREFIX + configuration.getIdps().getEmployeeRoleSystemAdministrator();
		final String employeeRoleReadOnly = ROLE_PREFIX + configuration.getIdps().getEmployeeRoleReadOnly();

		for (SamlGrantedAuthority authority : tokenUser.getAuthorities()) {
			String name = (String) tokenUser.getAttributes().get("name");
			String email = (String) tokenUser.getAttributes().get("email");
			if (employeeRoleSystemAdministrator.equals(authority.getAuthority())) {
				authorities.add(new SamlGrantedAuthority(Roles.ROLE_SYSTEM_ADMINISTRATOR, authority.getConstraints()));

				handleAdminLogin(tokenUser.getUsername(), name, email, Roles.ROLE_SYSTEM_ADMINISTRATOR);
			}
			else if (employeeRoleReadOnly.equals(authority.getAuthority())) {
				authorities.add(new SamlGrantedAuthority(Roles.ROLE_SYSTEM_READONLY, authority.getConstraints()));

				handleAdminLogin(tokenUser.getUsername(), name, email, Roles.ROLE_SYSTEM_READONLY);
			}
			else {
				log.error("Unknown role: " + authority.getAuthority());
			}
		}
	}

	private void handleMitIdLogin(TokenUser tokenUser, List<SamlGrantedAuthority> authorities) {
		if (tokenUser.getAttributes() == null || tokenUser.getAttributes().isEmpty()) {
			throw new SecurityUtilRuntimeException("Ugyldig SAML token fra NemLog-in - ingen attributter");
		}

		String uuid = (String) tokenUser.getAttributes().get("https://data.gov.dk/model/core/eid/professional/uuid/persistent");
		if (StringUtils.hasText(uuid)) {
			handleMitIdProfessionalLogin(tokenUser, authorities, uuid); // MitId Erhverv
		}
		else {
			handleMitIdPersonalLogin(tokenUser, authorities); // MitId Privat
		}
	}

	private void handleMitIdPersonalLogin(TokenUser tokenUser, List<SamlGrantedAuthority> authorities) {
		String cpr = (String) tokenUser.getAttributes().get("https://data.gov.dk/model/core/eid/cprNumber");
		if (!org.springframework.util.StringUtils.hasText(cpr)) {
			throw new SecurityUtilRuntimeException("Ugyldig SAML token fra NemLog-in - hverken cpr eller professional uuid attributter");
		}
		Employee employee = employeeService.findByCpr(cpr);

		// If the name changed since the last login, overwrite it
		overwriteEmployee(tokenUser, employee);
		authorities.add(new SamlGrantedAuthority(Roles.ROLE_EMPLOYEE));
	}

	private void handleMitIdProfessionalLogin(TokenUser tokenUser, List<SamlGrantedAuthority> authorities, String uuid) {
		String loginType = (String) getServletRequest().getSession().getAttribute("type");
		String cvr = (String) tokenUser.getAttributes().get("https://data.gov.dk/model/core/eid/professional/cvr");

		if (loginType.equals("leader")) {
			List<Constraint> constraints = new ArrayList<>();

			// constraints on agency CVRs
			StringBuilder builder = new StringBuilder();
			if (cvr != null) {
				builder.append(cvr);
			}

			if (!builder.isEmpty()) {
				Constraint constraint = new Constraint();
				constraint.setConstraintType(configuration.getIdps().getCvrConstraint());
				constraint.setConstraintValue(builder.toString());
				constraints.add(constraint);
			}

			authorities.add(new SamlGrantedAuthority(Roles.ROLE_EMPLOYEE_ADMINISTRATOR, constraints));
		}
		else {
			if (!org.springframework.util.StringUtils.hasText(uuid)) {
				throw new SecurityUtilRuntimeException("Bruger med UUID " + uuid + " ikke kendt i systemet");
			}
			Employee employee = employeeService.findByMitIdUuid(uuid);

			// If the name or email changed since the last login, overwrite it
			overwriteEmployee(tokenUser, employee);
			authorities.add(new SamlGrantedAuthority(Roles.ROLE_EMPLOYEE));
		}
	}

	public void handleAdminLogin(String uuid, String name, String email, String role) {
		Admin admin = adminService.findByUuid(uuid);

		if (admin == null) {
			admin = new Admin();
			admin.setUuid(uuid);
		}

		admin.setName(name);
		admin.setEmail(email);
		admin.setRole(role);

		adminService.save(admin);
	}

	private void overwriteEmployee(TokenUser tokenUser, Employee employee) {
		boolean hasChanges = false;

		if (employee != null) {
			if (tokenUser.getAttributes().get("name") != null && !Objects.equals(employee.getName(), tokenUser.getAttributes().get("name").toString())) {
				employee.setName(tokenUser.getAttributes().get("name").toString());
				hasChanges = true;
			}
			if (tokenUser.getAttributes().get("email") != null && !Objects.equals(employee.getEmail(), tokenUser.getAttributes().get("email").toString())) {
				employee.setEmail(tokenUser.getAttributes().get("email").toString());
				hasChanges = true;
			}
			if (hasChanges) {
				employeeService.save(employee);
			}
			employee.setLastLogin(LocalDateTime.now());
		}
	}
}
