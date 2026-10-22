package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.exception.ADOperationException;
import dk.digitalidentity.OS2ilm.model.entity.ActivationTimeFrame;
import dk.digitalidentity.OS2ilm.model.entity.AdGroup;
import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.exception.InvalidAccountNameException;
import dk.digitalidentity.OS2ilm.model.entity.enums.AccountStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.CmsPlaceHolder;
import dk.digitalidentity.OS2ilm.model.entity.EmailTemplate;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplateType;
import dk.digitalidentity.OS2ilm.model.entity.enums.SofdSyncStatus;
import dk.digitalidentity.OS2ilm.util.CprUtil;
import dk.digitalidentity.framework.ad.service.ActiveDirectoryService;
import dk.digitalidentity.framework.ad.service.model.CreateAccountRequest;
import dk.digitalidentity.framework.ad.service.model.CreateAccountResponse;
import dk.digitalidentity.framework.ad.service.model.DeleteAccountRequest;
import dk.digitalidentity.framework.ad.service.model.DeleteAccountResponse;
import dk.digitalidentity.framework.ad.service.model.DisableAccountRequest;
import dk.digitalidentity.framework.ad.service.model.DisableAccountResponse;
import dk.digitalidentity.framework.ad.service.model.EnableAccountRequest;
import dk.digitalidentity.framework.ad.service.model.EnableAccountResponse;
import dk.digitalidentity.framework.ad.service.model.ForcePasswordChangeRequest;
import dk.digitalidentity.framework.ad.service.model.ForcePasswordChangeResponse;
import dk.digitalidentity.framework.ad.service.model.GetGroupsReponse;
import dk.digitalidentity.framework.ad.service.model.GetGroupsRequest;
import dk.digitalidentity.framework.ad.service.model.RemoveGroupRequest;
import dk.digitalidentity.framework.ad.service.model.RemoveGroupResponse;
import dk.digitalidentity.framework.ad.service.model.SetAttributeRequest;
import dk.digitalidentity.framework.ad.service.model.SetAttributeResponse;
import dk.digitalidentity.framework.ad.service.model.SetExpireRequest;
import dk.digitalidentity.framework.ad.service.model.SetExpireResponse;
import dk.digitalidentity.framework.ad.service.model.SetGroupRequest;
import dk.digitalidentity.framework.ad.service.model.SetGroupResponse;
import dk.digitalidentity.framework.ad.service.model.SetPasswordRequest;
import dk.digitalidentity.framework.ad.service.model.SetPasswordResponse;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActiveDirectorySyncService {

	private final ActiveDirectoryService activeDirectoryService;
	private final AdGroupService adGroupService;
	private final AdUserService adUserService;
	private final IlmConfiguration configuration;
	private final EmployeeService employeeService;
	private final EmailTemplateService emailTemplateService;
	private final MessageService messageService;

	public static final String DATE_FORMAT = "yyyy/MM/dd";
	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
	private final ActivationTimeFrameService activationTimeFrameService;
	private final AccountNameValidator accountNameValidator;
	private final BusinessService businessService;

	@Transactional
	public void syncGroups() {
		String rootGroup = configuration.getActiveDirectoryConfig().getRootGroup();
		if (rootGroup == null || rootGroup.isBlank()) {
			throw new ADOperationException("os2ilm.activeDirectoryConfig.rootGroup is not configured");
		}

		GetGroupsRequest groupsRequest = new GetGroupsRequest();
		groupsRequest.setRootGroup(rootGroup);
		GetGroupsReponse response = activeDirectoryService.getGroups(groupsRequest);
		if (response.getGroups() == null || response.getGroups().isEmpty()) {
			log.warn("No groups found in AD");
			return;
		}

		log.info("Found " + response.getGroups().size() + " groups in AD");

		List<AdGroup> allGroups = adGroupService.findAll();
		List<String> existingGroups = allGroups.stream()
				.map(AdGroup::getName)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(ArrayList::new));

		List<AdGroup> tobeCreated = new ArrayList<>();
		response.getGroups().forEach(group -> {
			if (!existingGroups.contains(group.getName())) {
				AdGroup newGroup = new AdGroup();
				newGroup.setName(group.getName());
				tobeCreated.add(newGroup);
				
				log.info("Adding group: " + newGroup.getName());
			}

			existingGroups.remove(group.getName());
		});

		if (!tobeCreated.isEmpty()) {
			log.info("Adding {} new groups", tobeCreated.size());
			adGroupService.saveAll(tobeCreated);
		}

		if (!existingGroups.isEmpty()) {
			List<AdGroup> toDelete = allGroups.stream().filter(group -> existingGroups.contains(group.getName())).toList();
			log.info("Deleting {} existing groups", toDelete.size());
			adGroupService.deleteAll(toDelete);
		}
	}

	@Transactional
	public void checkForActivationsAndDeactivations() {
		LocalDate today = LocalDate.now();
		LocalTime currentTime = LocalTime.now();

		// Fetch ad users that need to be enabled and disabled from DB
		List<AdUser> potentialEnableUsers = adUserService.findByStartDateLessThanEqualAndEnabledFalse(today);
		List<AdUser> potentialDisableUsers = adUserService.findByStopDateLessThanEqualAndEnabledTrue(today);

		List<AdUser> needToBeEnabled = new ArrayList<>();
		List<AdUser> needToBeDisabled = new ArrayList<>();


		for (AdUser adUser : potentialEnableUsers) {
			if (shouldActivateUser(adUser, today, currentTime)) {
				needToBeEnabled.add(adUser);
			}
		}

		for (AdUser adUser : potentialDisableUsers) {
			if (shouldDeactivateUser(adUser, today, currentTime)) {
				needToBeDisabled.add(adUser);
			}
		}

		List<AdUser> saveList = new ArrayList<>();

		for (AdUser adUser : needToBeEnabled) {
			EnableAccountRequest request = new EnableAccountRequest();
			request.setUserId(adUser.getName());
			EnableAccountResponse response = activeDirectoryService.enableAccount(request);
			if (response == null || !response.isSuccess()) {
				String message = response == null ? "null response" : response.getMessage();
				log.warn("Failed to enable AD account for user {}: {} - leaving local state unchanged", adUser.getName(), message);
				continue;
			}
			adUser.setEnabled(true);
			saveList.add(adUser);
		}
		log.debug("Enabled {} users", saveList.size());

		int enabledCount = saveList.size();
		for (AdUser adUser : needToBeDisabled) {
			DisableAccountRequest request = new DisableAccountRequest();
			request.setUserId(adUser.getName());
			DisableAccountResponse response = activeDirectoryService.disableAccount(request);
			if (response == null || !response.isSuccess()) {
				String message = response == null ? "null response" : response.getMessage();
				log.warn("Failed to disable AD account for user {}: {} - leaving local state unchanged", adUser.getName(), message);
				continue;
			}
			adUser.setEnabled(false);
			saveList.add(adUser);
		}
		log.debug("Disabled {} users", saveList.size() - enabledCount);

		// Save in bulk for less database calls - only users whose AD call succeeded
		log.debug("Updated a total of {} users", saveList.size());
		adUserService.saveAll(saveList);
	}

	public boolean shouldActivateUser(AdUser adUser, LocalDate today, LocalTime currentTime) {
		ActivationTimeFrame timeFrame = adUser.getActivationTimeFrame();
		LocalDate startDate = adUser.getStartDate();
		LocalDate stopDate = adUser.getStopDate();

		// Check that we are within the date range
		if ((startDate != null && today.isBefore(startDate)) || (stopDate != null && today.isAfter(stopDate))) {
			return false;
		}

		// If no time frame, activate if within date range
		if (timeFrame == null) {
			return true;
		}

		// Check if we exclude weekends and today is a weekend
		if (timeFrame.isExcludeWeekends() && isWeekend(today)) {
			return false;
		}

		return isWithinActivationTimeWindow(timeFrame, currentTime);
	}

	private boolean shouldDeactivateUser(AdUser adUser, LocalDate today, LocalTime currentTime) {
		ActivationTimeFrame timeFrame = adUser.getActivationTimeFrame();
		LocalDate startDate = adUser.getStartDate();
		LocalDate stopDate = adUser.getStopDate();

		// Deactivate if we are outside of date range
		if ((startDate != null && today.isBefore(startDate)) || (stopDate != null && today.isAfter(stopDate))) {
			return true;
		}

		// If no time frame is configured, keep active if within date range
		if (timeFrame == null) {
			return false;
		}

		// Deactivate if weekends are excluded and today is a weekend
		if (timeFrame.isExcludeWeekends() && isWeekend(today)) {
			return true;
		}

		// Deactivate if outside the daily time window
		return !isWithinActivationTimeWindow(timeFrame, currentTime);
	}

	private boolean isWithinActivationTimeWindow(ActivationTimeFrame timeFrame, LocalTime currentTime) {
		LocalTime startTime = timeFrame.getStartTime();
		LocalTime stopTime = timeFrame.getStopTime();

		// Check start time constraint
		boolean afterStart = !currentTime.isBefore(startTime);

		// Check stop time constraint
		boolean beforeStop = currentTime.isBefore(stopTime);

		return afterStart && beforeStop;
	}

	private boolean isWeekend(LocalDate date) {
		return date.getDayOfWeek().getValue() >= 6;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AdUser createAccount(Long employeeId, Long activationTimeFrame, String name) {
		try {
			accountNameValidator.validate(name);
		} catch (InvalidAccountNameException ex) {
			throw new ADOperationException("The AD account name '" + name + "' is invalid: " + ex.getMessage(), ex);
		}

		Employee employee = employeeService.findById(employeeId);
		if (employee == null) {
			throw new ADOperationException("No employee with id " + employeeId);
		}

		AdUser existingUser = adUserService.findByEmployee(employee);
		if (existingUser != null) {
			throw new ADOperationException("The employee: " + employee.getName() + " already owns an active directory account " + existingUser.getName());
		}

		AdUser user = new AdUser();

		ActivationTimeFrame activation = null;
		if (activationTimeFrame != null) {
			activation = activationTimeFrameService.findById(activationTimeFrame);
		}

		String targetOu = configuration.getActiveDirectoryConfig().getTargetOu();
		if (targetOu == null || targetOu.isBlank()) {
			throw new ADOperationException("os2ilm.activeDirectoryConfig.targetOu is not configured");
		}

		CreateAccountRequest request = new CreateAccountRequest();
		request.setName(employee.getName());
		request.setUserName(name);
		request.setTargetOU(targetOu);
		request.setAdditionalAttributes(buildAdditionalAttributes(employee, name));

		CreateAccountResponse response = activeDirectoryService.createAccount(request);

		if (response == null || (response.getStatus() != null ? response.getStatus() != CreateAccountResponse.CreateStatus.OK : !response.isSuccess())) {
			String message = "";
			if (response != null) {
				message = response.getMessage() != null ? response.getMessage() : "";
			}
			throw new ADOperationException("The AD account could not be created: " + message);
		}

		// The call was successful, so we update user and employee locally
		AdUser savedUser = preCreateAdUser(employee, user, activation, name);

		employee.setUser(savedUser);
		employee.setPendingAccountName(null);
		employeeService.save(employee);
		log.info("Successfully created AD account: {}", name);

		return savedUser;
	}

	private Map<String, String> buildAdditionalAttributes(Employee employee, String userName) {
		Map<String, String> attributes = new HashMap<>();

		String displayName = employee.getName();
		if (displayName != null && !displayName.isBlank()) {
			attributes.put("displayName", displayName);
		}
		else {
			log.warn("Employee {} has no name, skipping displayName for AD account {}", employee.getId(), userName);
		}

		String upn = makeUpn(userName);
		if (upn != null) {
			attributes.put("userPrincipalName", upn);
		}
		else {
			log.debug("No UPN domain configured, skipping userPrincipalName for AD account {}", userName);
		}

		return attributes;
	}

	private String makeUpn(String userName) {
		String upnDomain = configuration.getActiveDirectoryConfig().getUpnDomain();
		if (upnDomain == null || upnDomain.isBlank()) {
			return null;
		}

		String domain = upnDomain.strip();
		if (domain.startsWith("@")) {
			domain = domain.substring(1);
		}

		return domain.isBlank() ? null : userName + "@" + domain;
	}

	private AdUser preCreateAdUser(Employee employee, AdUser user, ActivationTimeFrame activation, String name) {
		// Configuration-based, there is a prefix we need to make some logic on to make it look proper
		user.setName(name);
		user.setStartDate(employee.getStartDate());
		user.setStopDate(employee.getStopDate());
		user.setEmployee(employee);
		user.setStatus(AccountStatus.IN_USE);
		user.setSofdSyncStatus(SofdSyncStatus.CREATED);
		user.setActivationTimeFrame(activation);

		// If we can set a stop date AND create the account, we save
		return adUserService.save(user);
	}

	@Transactional
	public void setGroupMembership(Long employeeId, Long groupId) {
		Employee employee = employeeService.findById(employeeId);

		AdUser user = employee.getUser();
		if (user == null) {
			throw new ADOperationException("Employee " + employee.getName() + " does not have AD account");
		}

		AdGroup adGroup = adGroupService.findById(groupId);
		if (adGroup == null) {
			throw new ADOperationException("AdGroup not found: " + groupId);
		}

		SetGroupRequest request = new SetGroupRequest();
		request.setGroup(adGroup.getName());
		request.setUserId(user.getName());

		SetGroupResponse response = activeDirectoryService.setGroup(request);
		if (response == null || !response.isSuccess()) {
			String message = response == null ? "null response" : response.getMessage();
			throw new ADOperationException("Failed to set AD group: " + message);
		}

		// AD operation succeeded - ensure group IS in DB
		if (!employee.getAdGroups().contains(adGroup)) {
			employee.addAdGroup(adGroup);
			employeeService.save(employee);
			log.info("Added group {} to employee {} in DB after successful AD operation",
					groupId, employee.getId());
		}
	}

	@Transactional
	public void deleteGroupMembership(Long employeeId, long groupId) {
		Employee employee = employeeService.findById(employeeId);

		AdUser user = employee.getUser();
		if (user == null) {
			throw new ADOperationException("Employee " + employee.getName() + " does not have AD account");
		}

		AdGroup adGroup = adGroupService.findById(groupId);
		if (adGroup == null) {
			throw new ADOperationException("AdGroup not found: " + groupId);
		}

		RemoveGroupRequest request = new RemoveGroupRequest();
		request.setGroup(adGroup.getName());
		request.setUserId(user.getName());

		RemoveGroupResponse removeGroupResponse = activeDirectoryService.removeGroup(request);
		if (removeGroupResponse == null || !removeGroupResponse.isSuccess()) {
			String message = removeGroupResponse == null ? "null response" : removeGroupResponse.getMessage();
			throw new ADOperationException("Failed to remove AD Group: " + message);
		}

		// AD operation succeeded - ensure group is NOT in DB
		if (employee.getAdGroups().contains(adGroup)) {
			employee.getAdGroups().remove(adGroup);
			employeeService.save(employee);
			log.info("Removed group {} from employee {} in DB after successful AD operation",
					groupId, employee.getId());
		}
		adUserService.save(user);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void setExpireDate(Long employeeId, LocalDate stopDate) throws ADOperationException {
		Employee employee = employeeService.findById(employeeId);
		if (employee == null) {
			throw new ADOperationException("Could not find employee with ID " + employeeId);
		}

		AdUser user = employee.getUser();
		if (user == null) {
			throw new ADOperationException("The employee: " + employee.getName() + " does not have an active directory account!");
		}

		SetExpireRequest expireRequest = new SetExpireRequest();
		expireRequest.setUserId(user.getName());
		expireRequest.setTts(stopDate.format(DATE_TIME_FORMATTER));

		SetExpireResponse setExpireResponse = activeDirectoryService.setExpire(expireRequest);

		if (setExpireResponse == null || !setExpireResponse.isSuccess()) {
			String message = setExpireResponse == null ? "null response" : setExpireResponse.getMessage();
			throw new ADOperationException("The expire date could not be set: " + message);
		}

		// The call was successful, so we update the user and employee locally
		employee.setStopDate(stopDate);
		employeeService.save(employee);

		user.setStopDate(stopDate);
		adUserService.save(user);

		log.info("Successfully updated the expireDate on AD account: {}", user.getName());
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void disableAccount(Long adUserId) throws ADOperationException {
		AdUser user = adUserService.findById(adUserId);
		if (user == null) {
			throw new ADOperationException("Could not find AD user with ID " + adUserId);
		}

		DisableAccountRequest request = new DisableAccountRequest();
		request.setUserId(user.getName());

		DisableAccountResponse response = activeDirectoryService.disableAccount(request);
		if (response == null || !response.isSuccess()) {
			String message = response == null ? "null response" : response.getMessage();
			throw new ADOperationException("The AD account could not be disabled: " + message);
		}

		LocalDate today = LocalDate.now();

		// Also expire the account in AD itself - disabling alone leaves the expiration date at "never",
		// same setExpire call the lock/extend flows use via setExpireDate().
		SetExpireRequest expireRequest = new SetExpireRequest();
		expireRequest.setUserId(user.getName());
		expireRequest.setTts(today.format(DATE_TIME_FORMATTER));

		SetExpireResponse expireResponse = activeDirectoryService.setExpire(expireRequest);
		if (expireResponse == null || !expireResponse.isSuccess()) {
			String message = expireResponse == null ? "null response" : expireResponse.getMessage();
			throw new ADOperationException("The AD account was disabled but its expire date could not be set: " + message);
		}

		user.setEnabled(false);

		// Without a past stopDate, checkForActivationsAndDeactivations() would see startDate <= today and
		// enabled = false on its next run and treat this account as merely pending activation, re-enabling
		// it right back. A stopDate strictly before today keeps shouldActivateUser() returning false.
		user.setStopDate(today.minusDays(1));
		adUserService.save(user);

		log.info("Successfully disabled AD account: {}", user.getName());
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void deleteAccount(Long adUserId) throws ADOperationException {
		AdUser user = adUserService.findById(adUserId);
		if (user == null) {
			throw new ADOperationException("Could not find AD user with ID " + adUserId);
		}

		DeleteAccountRequest request = new DeleteAccountRequest();
		request.setUserId(user.getName());

		DeleteAccountResponse response = activeDirectoryService.deleteAccount(request);
		if (response == null || !response.isSuccess()) {
			String message = response == null ? "null response" : response.getMessage();
			throw new ADOperationException("The AD account could not be deleted: " + message);
		}

		adUserService.delete(user);
		log.info("Successfully deleted AD account: {}", user.getName());
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void setPassword(Long employeeId, String password, boolean sendNotificationEnabled) {
		Employee employee = employeeService.findById(employeeId);
		if (employee == null) {
			throw new ADOperationException("Could not find employee with ID " + employeeId);
		}

		AdUser user = employee.getUser();
		if (user == null) {
			throw new ADOperationException("The employee: " + employee.getName() + " does not have an active directory account!");
		}

		SetPasswordRequest request = new SetPasswordRequest();
		request.setUserId(user.getName());
		request.setPassword(password);

		SetPasswordResponse response = activeDirectoryService.setPassword(request);
		if (response == null || response.getStatus() != SetPasswordResponse.PasswordStatus.OK) {
			String message = response == null ? "null response" : response.getMessage();
			throw new ADOperationException("The password could not be set! " + message);
		}

		// AD operation succeeded - notify the user. Done here (rather than in the controller) so the email is only
		// sent when AD has actually accepted the new password, not merely when the request was enqueued.
		if (sendNotificationEnabled) {
			EmailTemplate emailTemplate = emailTemplateService.getByType(EmailTemplateType.EMPLOYEE_PASSWORD_RESET);
			if (emailTemplate.isEnabled()) {
				EmailTemplateService.RenderedEmail email = emailTemplateService.render(emailTemplate, Map.of(
						EmailTemplatePlaceholder.RECIPIENT, employee.getName(),
						EmailTemplatePlaceholder.PASSWORD, password));
				messageService.sendNotification(employee, email);
			}
		}

		// The call was successful, so we update the user - We always update the initialPasswordSet flag since it does not matter
		// It's only important that the flag is reverted to false in the initial call if something goes wrong.
		user.setInitialPasswordSet(true);
		adUserService.save(user);

		log.info("Successfully set the password on AD account: {}", user.getName());
	}

	public void forcePasswordChange(Employee employee) {
		AdUser user = employee.getUser();
		if (user == null) {
			throw new ADOperationException("The employee: " + employee.getName() + " does not have an active directory account!");
		}

		ForcePasswordChangeRequest request = new ForcePasswordChangeRequest();
		request.setUserId(user.getName());

		ForcePasswordChangeResponse response = activeDirectoryService.setForcePasswordChange(request);
		if (response == null || !response.isSuccess()) {
			String message = response == null ? "null response" : response.getMessage();
			throw new ADOperationException("Failed to force password change: " + message);
		}

		log.info("Successfully changed the password on AD account: {}", user.getName());
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void setCompanyAttribute(Long employeeId, Long businessId) {
		AdUser user = getAdUser(employeeId);
		String userId = user.getName();

		Business business = businessService.findById(businessId);
		if (business == null) {
			throw new ADOperationException("Could not find business with ID " + businessId);
		}

		String label = "company";
		String businessName = business.getName();

		String companyAttribute = configuration.getActiveDirectoryConfig().getCompanyAttribute();
		if (shouldSkip(companyAttribute, businessName, label, userId)) {
			return;
		}

		writeAttribute(userId, companyAttribute, businessName, label);

		// The call was successful, so we update the user locally
		user.setBusiness(business);
		adUserService.save(user);

		log.info("Successfully wrote company to AD attribute '{}' for user {}", companyAttribute, userId);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void setPhoneAttribute(Long employeeId, String phoneNumber) {
		AdUser user = getAdUser(employeeId);
		String userId = user.getName();

		String label = "phonenumber";
		String phoneAttribute = configuration.getActiveDirectoryConfig().getPhoneAttribute();

		if (shouldSkip(phoneAttribute, phoneNumber, label, userId)) {
			return;
		}

		writeAttribute(userId, phoneAttribute, phoneNumber, label);

		// The call was successful, so we update the user locally
		user.setPhoneNumber(phoneNumber);
		adUserService.save(user);

		log.info("Successfully wrote phonenumber to AD attribute '{}' for user {}", phoneAttribute, userId);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void setCprAttribute(Long employeeId, String cpr) {
		AdUser user = getAdUser(employeeId);
		String userId = user.getName();

		String label = "CPR";
		String cprAttribute = configuration.getActiveDirectoryConfig().getCprAttribute();

		if (shouldSkip(cprAttribute, cpr, label, userId)) {
			return;
		}

		// Lax on the way out: a CPR we cannot make sense of is still written to AD unchanged, so accounts
		// carrying pre-existing data keep working. Strict on the way in: AdUser.cpr doubles as the "CPR is
		// done" flag for the CREATE sequence and is always stored normalized, independent of the configured
		// output format.
		String formattedCpr = CprUtil.format(cpr, configuration.getActiveDirectoryConfig().isCprWithDash());
		if (!CprUtil.isValid(cpr)) {
			log.warn("CPR for user {} is not 10 characters long, writing it to AD unchanged", userId);
		}

		writeAttribute(userId, cprAttribute, formattedCpr, label);

		// The call was successful, so we update the user locally
		user.setCpr(CprUtil.normalize(cpr));
		adUserService.save(user);

		log.info("Successfully wrote CPR to AD attribute '{}' for user {}", cprAttribute, userId);
	}

	// Resolves the AD user for an employee, or throws
	private AdUser getAdUser(Long employeeId) {
		Employee employee = employeeService.findById(employeeId);
		if (employee == null) {
			throw new ADOperationException("Could not find employee with ID " + employeeId);
		}

		AdUser user = employee.getUser();
		if (user == null) {
			throw new ADOperationException("The employee: " + employee.getName() + " does not have an active directory account");
		}
		return user;
	}

	// True if attribute isn't configured or there's no value to write
	private boolean shouldSkip(String attribute, String value, String label, String userId) {
		if (attribute == null || attribute.isBlank()) {
			log.debug("No {} attribute configured, skipping {} write for user {}", label, label, userId);
			return true;
		}
		if (value == null || value.isBlank()) {
			log.warn("No {} available for user {}, skipping {} attribute write", label, userId, label);
			return true;
		}
		return false;
	}

	// Writes a single attribute to AD, throws on failure
	private void writeAttribute(String userId, String attribute, String value, String label) {
		SetAttributeRequest request = new SetAttributeRequest();
		request.setUserId(userId);
		request.setField(attribute);
		request.setValue(value);

		SetAttributeResponse response = activeDirectoryService.setAttribute(request);
		if (response == null || !response.isSuccess()) {
			String message = response == null ? "" : response.getMessage();
			log.warn("Could not set {} attribute for user {}: {}", label, userId, message);
			throw new ADOperationException("Could not set " + label + " attribute for user " + userId + ": " + message);
		}
	}

	public String makeAdUserName(Employee employee) {
		// Compute the prefix which includes the configured global prefix and the placeholder that each Business has.
		String basePrefix = configuration.getActiveDirectoryConfig().getPrefix();
		String usernamePrefix = basePrefix.replace(CmsPlaceHolder.PLACEHOLDER_BUSINESS_PREFIX.name(), employee.getBusiness().getPrefix());

		// Extract existing users by prefix and find the max already used number
		List<AdUser> existingUsers = adUserService.findByNameStartingWith(usernamePrefix);
		int maxNumber = existingUsers.stream()
				.map(AdUser::getName)
				.map(username -> username.substring(usernamePrefix.length()))
				.filter(suffix -> suffix.matches("\\d+"))
				.mapToInt(Integer::parseInt)
				.max()
				.orElse(0);

		String numberSuffix = String.format("%04d", maxNumber + 1);
		return usernamePrefix + numberSuffix;
	}
}
