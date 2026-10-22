package dk.digitalidentity.OS2ilm.security;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.exception.SecurityUtilRuntimeException;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.Leader;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import dk.digitalidentity.OS2ilm.service.LeaderService;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.TokenUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority.Constraint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityUtil {
	public static final String ROLE_PREFIX = "ROLE_";
	private final IlmConfiguration configuration;
	private final EmployeeService employeeService;
	private final BusinessService businessService;
	private final LeaderService leaderService;
	private final AuditLogService auditLogService;

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
			throw new SecurityUtilRuntimeException("Cant fetch TokenUser - Not logged in!");
		}

		return (TokenUser) SecurityContextHolder.getContext().getAuthentication().getDetails();
	}

	/**
	 * Returns true if the current TokenUser has the supplied role
	 */
	public boolean hasRole(String role) {
		if (notAuthenticated()) {
			return false;
		}

		return getTokenUser().getAuthorities().stream().anyMatch(ga -> Objects.equals(ga.getAuthority().toUpperCase(), role));
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
		return hasRole(Roles.ROLE_LEADER);
	}

	/**
	 * returns true if the logged-in employee exists, has accepted the TOS and is not awaiting approval
	 */
	public boolean isActiveEmployee() {
		return getEmployee()
				.map(e -> e.isAcceptedTos() && e.getApproved() != ApproveStatus.AWAITING_APPROVAL)
				.orElse(false);
	}

	/**
	 * returns the redirect the employee must be sent to before accessing employee pages, or empty if access is allowed
	 */
	public Optional<String> getEmployeeRedirect(Employee employee) {
		if (!employee.isAcceptedTos()) {
			return Optional.of("redirect:/registration/policy");
		}
		if (employee.getApproved() == ApproveStatus.AWAITING_APPROVAL) {
			return Optional.of("redirect:/registration/awaitingApproval/" + employee.getId());
		}
		return Optional.empty();
	}

	// Checks whether an employee needs to register first
	public boolean isFirstTimeLogin() {
		return getEmployee().isEmpty();
	}

	public boolean hasNoAccessToEmployee(Employee employee) {
		if (isAdmin() || isReadOnlyAdmin()) {
			return false;
		}
		List<String> cvrs = getCvrs();
		if (cvrs.isEmpty()) {
			return true;
		}
		return employee == null || !cvrs.contains(employee.getCvr());
	}

	public boolean hasNoAccessToBusiness(long businessId) {
		if (isAdmin() || isReadOnlyAdmin()) {
			return false;
		}
		List<String> cvrs = getCvrs();
		if (cvrs.isEmpty()) {
			return true;
		}
		Business business = businessService.findById(businessId);
		return business == null || !cvrs.contains(business.getCvr());
	}

	public String getPerformerId() {
		return getMitIdUuid().orElse(getTokenUser().getUsername());
	}

	public String getCvrCSV() {
		final TokenUser tokenUser = getTokenUser();
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
			log.debug("Constraints: {}", collect);
		}
		return collect;
	}

	public Optional<Employee> getEmployee() {
		final Optional<String> cpr = getCPRAttribute();
		final Optional<String> uuid = getMitIdUuid();
		if(cpr.isPresent()) {
			return Optional.ofNullable(employeeService.findByCpr(cpr.get()));
		} else if (uuid.isPresent()) {
			return Optional.ofNullable(employeeService.findByMitIdUuid(uuid.get()));
		}

		return Optional.empty();
	}

	public Optional<String> getCPRAttribute() {
		final TokenUser tokenUser = getTokenUser();

		final Object cprObj = tokenUser.getAttributes().get(SecurityConstants.PERSONAL_CPR);
		if (cprObj instanceof String cpr) {
			return Optional.of(cpr);
		}
		else {
			return Optional.empty();
		}
	}

	public Optional<String> getMitIdUuid() {
		final TokenUser tokenUser = getTokenUser();

		final Object mitIdUuidObj = tokenUser.getAttributes().get(SecurityConstants.PROFESSIONAL_UUID_PERSISTANT);
		if (mitIdUuidObj instanceof String uuid) {
			return Optional.of(uuid);
		} else {
			return Optional.empty();
		}
	}

	public String getName() {
		final Map<String, Object> attributes = getTokenUser().getAttributes();
		if (attributes.isEmpty()) {
			return null;
		}

		final String name = (String) attributes.get("name");
		if (org.springframework.util.StringUtils.hasText(name)) {
			return name;
		}

		return (String) attributes.get(SecurityConstants.USERNAME);
	}

	public Optional<String> getEmail() {
		final Map<String, Object> attributes = getTokenUser().getAttributes();

		Object emailObj = attributes.get("email");
		if (emailObj == null) {
			emailObj = attributes.get(SecurityConstants.EMAIL);
		}

		if (emailObj instanceof String email && org.springframework.util.StringUtils.hasText(email)) {
			return Optional.of(email.trim());
		}

		return Optional.empty();
	}

	public boolean isPseudonymName(String name) {
		return SecurityConstants.PSEUDONYM_NAME.equalsIgnoreCase(name == null ? null : name.trim());
	}

	/**
	 * The name as verified by the identity provider, empty when the IdP did not supply a usable one.
	 * Callers may only lock down the name field when this holds a value.
	 */
	public Optional<String> getVerifiedName() {
		final String name = getName();
		if (!org.springframework.util.StringUtils.hasText(name) || isPseudonymName(name)) {
			return Optional.empty();
		}

		return Optional.of(name);
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
			log.warn("Ingen roller tildelt for bruger: {}", tokenUser.getUsername());
		}
		tokenUser.setAuthorities(authorities);
	}

	private void handleInternalLogin(TokenUser tokenUser, List<SamlGrantedAuthority> authorities) {
		final String employeeRoleSystemAdministrator = ROLE_PREFIX + configuration.getIdps().getEmployeeRoleSystemAdministrator();
		final String employeeRoleReadOnly = ROLE_PREFIX + configuration.getIdps().getEmployeeRoleReadOnly();
		final String employeeRoleLeader = ROLE_PREFIX + configuration.getIdps().getEmployeeRoleLeader();

		for (SamlGrantedAuthority authority : tokenUser.getAuthorities()) {
			String name = (String) tokenUser.getAttributes().get("name");
			String email = (String) tokenUser.getAttributes().get("email");
			if (employeeRoleSystemAdministrator.equals(authority.getAuthority())) {
				authorities.add(new SamlGrantedAuthority(Roles.ROLE_SYSTEM_ADMINISTRATOR, authority.getConstraints()));
			}
			else if (employeeRoleReadOnly.equals(authority.getAuthority())) {
				authorities.add(new SamlGrantedAuthority(Roles.ROLE_SYSTEM_READONLY, authority.getConstraints()));
			}
			else if (employeeRoleLeader.equals(authority.getAuthority())) {
				String uuid = tokenUser.getUsername();
				List<String> tokenCvrs = extractCvrsFromConstraints(authority.getConstraints());

				Leader leader = leaderService.findByInternalUuidWithBusinesses(uuid);

				if (configuration.isLeaderInternalIdpSyncEnabled()) {
					boolean isNewLeader = leader == null;
					if (isNewLeader) {
						leader = new Leader();
						leader.setInternalUuid(uuid);
					}
					leader.setName(name);
					leader.setMail(email);
					leaderService.syncBusinessesFromCvrs(leader, tokenCvrs);

					if (leader.getBusinesses().isEmpty()) {
						log.warn("Leader login via intern IdP for uuid {} - none of the CVRs from token matched a known business: {}", uuid, tokenCvrs);
						throw new SecurityUtilRuntimeException("Ingen af virksomhederne fra login-token er registreret i systemet. Kontakt en administrator.");
					}

					Leader saved = leaderService.save(leader);

					List<String> businessNames = saved.getBusinesses().stream().map(Business::getName).toList();
					if (isNewLeader) {
						auditLogService.logSystemAction(saved.getId(), saved.getName(), LogAction.LEADER_CREATED,
								"Leder blev automatisk oprettet via intern IdP login",
								new LeaderLogRecord(saved.getId(), saved.getName(), saved.getMail(), saved.getIdentifier(), businessNames));
					} else {
						auditLogService.logSystemAction(saved.getId(), saved.getName(), LogAction.LEADER_UPDATED,
								"Leder blev automatisk opdateret via intern IdP login",
								new LeaderLogRecord(saved.getId(), saved.getName(), saved.getMail(), saved.getIdentifier(), businessNames));
					}
				} else if (leader == null) {
					throw new SecurityUtilRuntimeException("Lederen er ikke registreret i systemet. Kontakt en administrator.");
				}

				List<String> cvrs = leaderService.getLeaderCvrs(leader);
				List<Constraint> constraints = buildCvrConstraints(cvrs);
				authorities.add(new SamlGrantedAuthority(Roles.ROLE_LEADER, constraints));
			}
			else {
				log.error("Unknown role: {}", authority.getAuthority());
			}
		}
	}

	private void handleMitIdLogin(TokenUser tokenUser, List<SamlGrantedAuthority> authorities) {
		if (tokenUser.getAttributes() == null || tokenUser.getAttributes().isEmpty()) {
			throw new SecurityUtilRuntimeException("Ugyldig SAML token fra NemLog-in - ingen attributter");
		}

		String uuid = (String) tokenUser.getAttributes().get(SecurityConstants.PROFESSIONAL_UUID_PERSISTANT);
		if (StringUtils.hasText(uuid)) {
			handleMitIdProfessionalLogin(tokenUser, authorities, uuid); // MitId Erhverv
		}
		else {
			handleMitIdPersonalLogin(tokenUser, authorities); // MitId Privat
		}
	}

	private void handleMitIdPersonalLogin(TokenUser tokenUser, List<SamlGrantedAuthority> authorities) {
		String cpr = (String) tokenUser.getAttributes().get(SecurityConstants.PERSONAL_CPR);
		if (!org.springframework.util.StringUtils.hasText(cpr)) {
			throw new SecurityUtilRuntimeException("Ugyldig SAML token fra NemLog-in - hverken cpr eller professional uuid attributter");
		}

		String loginType = (String) getServletRequest().getSession().getAttribute("type");
		if ("leader".equals(loginType)) {
			handleMitIdPersonalLeaderLogin(tokenUser, authorities, cpr);
			return;
		}

		Employee employee = employeeService.findByCpr(cpr);

		// If the name changed since the last login, overwrite it
		overwriteEmployee(tokenUser, employee);
		authorities.add(new SamlGrantedAuthority(Roles.ROLE_EMPLOYEE));
	}

	private void handleMitIdPersonalLeaderLogin(TokenUser tokenUser, List<SamlGrantedAuthority> authorities, String cpr) {
		Leader leader = leaderService.findByCprWithBusinesses(cpr);
		if (leader == null) {
			throw new SecurityUtilRuntimeException("Lederen er ikke registreret i systemet med dette CPR-nummer. Kontakt en administrator.");
		}

		String name = (String) tokenUser.getAttributes().get("name");
		String mail = (String) tokenUser.getAttributes().get("email");
		if (StringUtils.hasText(name)) {
			leader.setName(name);
		}
		if (StringUtils.hasText(mail)) {
			leader.setMail(mail);
		}
		leaderService.save(leader);

		List<String> cvrs = leaderService.getLeaderCvrs(leader);
		List<Constraint> constraints = buildCvrConstraints(cvrs);
		authorities.add(new SamlGrantedAuthority(Roles.ROLE_LEADER, constraints));
	}

	private void handleMitIdProfessionalLogin(TokenUser tokenUser, List<SamlGrantedAuthority> authorities, String uuid) {
		String loginType = (String) getServletRequest().getSession().getAttribute("type");
		String cvr = (String) tokenUser.getAttributes().get(SecurityConstants.PROFESSIONAL_CVR);
		String cpr = (String) tokenUser.getAttributes().get(SecurityConstants.PERSONAL_CPR);

		String name = (String) tokenUser.getAttributes().get(SecurityConstants.USERNAME);
		String mail = (String) tokenUser.getAttributes().get(SecurityConstants.EMAIL);

		Business business = businessService.findBusinessByCvr(cvr);
		if (business == null) {
			throw new SecurityUtilRuntimeException("CVR-nummer ikke kendt i systemet. Virksomheder skal oprettes på forhånd.");
		}

		if (loginType.equals("leader")) {
			Leader leader = leaderService.findByMitIdUuidWithBusinesses(uuid);

			// Professionals can now also log in using cpr number
			if (leader == null && StringUtils.hasText(cpr)) {
				leader =  leaderService.findByCprWithBusinesses(cpr);
			}

			if (leader == null) {
				if (configuration.isLeaderAutoRegistrationEnabled()) {
					leader = new Leader();
					leader.setMitIdUuid(uuid);
					leader.setName(name);
					leader.setMail(mail);
					leader.getBusinesses().add(business);
					Leader saved = leaderService.save(leader);

					List<String> businessNames = saved.getBusinesses().stream().map(Business::getName).toList();
					auditLogService.logSystemAction(saved.getId(), saved.getName(), LogAction.LEADER_CREATED,
							"Leder blev automatisk oprettet via MitID Erhverv login",
							new LeaderLogRecord(saved.getId(), saved.getName(), saved.getMail(), saved.getIdentifier(), businessNames));
				} else {
					throw new SecurityUtilRuntimeException("Lederen er ikke registreret i systemet. Kontakt en administrator.");
				}
			} else {
				boolean changed = false;
				if (name != null && !name.isBlank()) {
					leader.setName(name);
					changed = true;
				}
				if (mail != null && !mail.isBlank()) {
					leader.setMail(mail);
					changed = true;
				}
				if (changed) {
					leaderService.save(leader);
				}
			}

			List<String> cvrs = leaderService.getLeaderCvrs(leader);
			List<Constraint> constraints = buildCvrConstraints(cvrs);
			authorities.add(new SamlGrantedAuthority(Roles.ROLE_LEADER, constraints));
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

	private List<String> extractCvrsFromConstraints(List<Constraint> constraints) {
		List<String> cvrs = new ArrayList<>();
		for (Constraint constraint : constraints) {
			if (constraint.getConstraintType().equals(configuration.getIdps().getCvrConstraint())) {
				String value = constraint.getConstraintValue();
				if (org.springframework.util.StringUtils.hasText(value)) {
					cvrs.addAll(Arrays.asList(value.replace(" ", "").split(",")));
				}
			}
		}
		return cvrs;
	}

	private List<Constraint> buildCvrConstraints(List<String> cvrs) {
		List<Constraint> constraints = new ArrayList<>();
		if (!cvrs.isEmpty()) {
			Constraint constraint = new Constraint();
			constraint.setConstraintType(configuration.getIdps().getCvrConstraint());
			constraint.setConstraintValue(String.join(",", cvrs));
			constraints.add(constraint);
		}
		return constraints;
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

	public record LeaderLogRecord(Long id, String name, String mail, String identifier, List<String> businesses) {}
}
