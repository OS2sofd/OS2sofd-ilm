package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.model.entity.ActivationTimeFrame;
import dk.digitalidentity.OS2ilm.model.entity.AdGroup;
import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.service.cms.CmsMessageBundle;
import dk.digitalidentity.OS2ilm.util.RandomPasswordGeneratorUtil;
import dk.digitalidentity.OS2ilm.model.entity.enums.AccountStatus;
import dk.digitalidentity.framework.ad.service.ActiveDirectoryService;
import dk.digitalidentity.framework.ad.service.model.CreateAccountRequest;
import dk.digitalidentity.framework.ad.service.model.CreateAccountResponse;
import dk.digitalidentity.framework.ad.service.model.DisableAccountRequest;
import dk.digitalidentity.framework.ad.service.model.EnableAccountRequest;
import dk.digitalidentity.framework.ad.service.model.ForcePasswordChangeRequest;
import dk.digitalidentity.framework.ad.service.model.ForcePasswordChangeResponse;
import dk.digitalidentity.framework.ad.service.model.GetGroupsReponse;
import dk.digitalidentity.framework.ad.service.model.RemoveGroupRequest;
import dk.digitalidentity.framework.ad.service.model.RemoveGroupResponse;
import dk.digitalidentity.framework.ad.service.model.SetExpireRequest;
import dk.digitalidentity.framework.ad.service.model.SetExpireResponse;
import dk.digitalidentity.framework.ad.service.model.SetGroupRequest;
import dk.digitalidentity.framework.ad.service.model.SetGroupResponse;
import dk.digitalidentity.framework.ad.service.model.SetPasswordRequest;
import dk.digitalidentity.framework.ad.service.model.SetPasswordResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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
	private final CmsMessageBundle cmsMessageBundle;
	private final MessageService messageService;

	public static final String DATE_FORMAT = "yyyy/MM/dd";
	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
	private final AuditLogService auditLogService;
	private final ActivationTimeFrameService activationTimeFrameService;

	@Transactional
	public void syncGroups() {
		List<AdGroup> allGroups = adGroupService.findAll();
		List<String> existingGroups = allGroups.stream()
				.map(AdGroup::getName)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(ArrayList::new));
		GetGroupsReponse response = activeDirectoryService.getGroups();
		List<AdGroup> groups = new ArrayList<>();
		response.getGroups().forEach(group -> {
			if (!existingGroups.contains(group.getName())) {
				AdGroup newGroup = new AdGroup();
				newGroup.setName(group.getName());
				groups.add(newGroup);
			}
			existingGroups.remove(group.getName());
		});
		if (!groups.isEmpty()) {
			log.debug("Adding {} new groups", groups.size());
			adGroupService.saveAll(groups);
		}
		if (!existingGroups.isEmpty()) {
			List<AdGroup> toDelete = allGroups.stream().filter(group -> existingGroups.contains(group.getName())).toList();
			log.debug("Deleting {} existing groups", toDelete.size());
			adGroupService.deleteAll(toDelete);
		}
	}

	@Transactional
	public void checkForActivationsAndDeactivations() {
		// Fetch ad users that need to be enabled and disabled from DB
		List<AdUser> needToBeEnabled = adUserService.findByStartDateBeforeAndEnabledFalse(LocalDate.now());
		List<AdUser> needToBeDisabled = adUserService.findByStopDateBeforeAndEnabledTrue(LocalDate.now());

		for (AdUser adUser : needToBeEnabled) {
			EnableAccountRequest request = new EnableAccountRequest();
			request.setUserId(adUser.getName());
			activeDirectoryService.enableAccount(request);
			adUser.setEnabled(true);
		}
		log.debug("Enabled {} users", needToBeEnabled.size());

		for (AdUser adUser : needToBeDisabled) {
			DisableAccountRequest request = new DisableAccountRequest();
			request.setUserId(adUser.getName());
			activeDirectoryService.disableAccount(request);
			adUser.setEnabled(false);
		}
		log.debug("Disabled {} users", needToBeDisabled.size());

		List<AdUser> saveList = new ArrayList<>();
		saveList.addAll(needToBeDisabled);
		saveList.addAll(needToBeEnabled);

		// Save in bulk for less database calls
		log.debug("Updated a total of {} users", saveList.size());
		adUserService.saveAll(saveList);
	}


	@Transactional
	public Employee createAdUser(Employee employee, long activationTimeFrame) {
		AdUser existingUser = adUserService.findByEmployee(employee);
		if (existingUser != null) {
			log.warn("The user employee: {} already owns an active directory account!", employee.getName());
			return null;
		}
		AdUser user = new AdUser();

		ActivationTimeFrame activation = activationTimeFrameService.findById(activationTimeFrame);
		if (activation == null) {
			log.warn("The activation time frame with id: {} could not be found!", activationTimeFrame);
			return null;
		}

		// Configuration based, there is a prefix we need to make some logic on to make it look proper
		String name = makeAdUserName(employee);
		user.setName(name);
		user.setExtUuid(UUID.randomUUID().toString());
		user.setStartDate(employee.getStartDate());
		user.setStopDate(employee.getStopDate());
		user.setEmployee(employee);
		user.setStatus(AccountStatus.IN_USE);
		user.setActivationTimeFrame(activation);

		// If we can set a stop date AND create the account, we save
		adUserService.save(user);

		CreateAccountRequest request = new CreateAccountRequest();
		request.setName(employee.getName());
		request.setUserName(name);

		CreateAccountResponse response = activeDirectoryService.createAccount(request);

		if (response == null || !response.getStatus().equals(CreateAccountResponse.CreateStatus.OK)) {
			String message = response == null ? "" : response.getMessage();
			log.warn("The AD account could not be created {}", message);
			return null;
		}

		SetExpireRequest expireRequest = new SetExpireRequest();
		expireRequest.setUserId(name);
		expireRequest.setTts(user.getStopDate().format(DATE_TIME_FORMATTER));

		SetExpireResponse setExpireResponse = activeDirectoryService.setExpire(expireRequest);
		if (setExpireResponse == null || !setExpireResponse.isSuccess()) {
			String message = setExpireResponse == null ? "" : setExpireResponse.getMessage();
			log.warn("The AD account could not be set to expire {}", message);
			return null;
		}

		String generatedPassword = RandomPasswordGeneratorUtil.generateUserFriendlyPassword(configuration.getActiveDirectoryConfig().getPasswordLength());
		SetPasswordRequest passwordRequest = new SetPasswordRequest();
		passwordRequest.setUserId(name);
		passwordRequest.setPassword(generatedPassword);

		SetPasswordResponse setPasswordResponse = activeDirectoryService.setPassword(passwordRequest);
		if (setPasswordResponse == null || !setPasswordResponse.getStatus().equals(SetPasswordResponse.PasswordStatus.OK)) {
			String message = setPasswordResponse == null ? "" : setPasswordResponse.getMessage();

			log.warn("The password of {} could not be set: {}", name, message);
		}
		// Update the employee
		employee.setUser(user);
		Employee savedEmployee = employeeService.save(employee);

		String message = cmsMessageBundle.getText("cms.employee.approved");
		message = message.replace("PLACEHOLDER_RECIPIENT", employee.getName());
		message = message.replace("PLACEHOLDER_USERNAME", user.getName());
		message = message.replace("PLACEHOLDER_PASSWORD", generatedPassword);

		messageService.sendNotification(employee, "OS2ILM: Anmodning godkendt", message);

		auditLogService.log(null, null, user.getEmployee().getId(), user.getEmployee().getName(), LogAction.AD_USER_CREATED, "Ny bruger oprettet i AD", user.getName());

		return savedEmployee;
	}

	@Transactional
	public void setGroupMembership(Employee employee, AdGroup adGroup) {
		AdUser  user = employee.getUser();
		if (user == null) {
			log.warn("The employee: {} does not have an active directory account!", employee.getName());
			return;
		}

//		user.addAdGroup(adGroup);
		adUserService.save(user);

		SetGroupRequest request = new SetGroupRequest();
		request.setGroup(adGroup.getName());
		request.setUserId(user.getName());

		SetGroupResponse response = activeDirectoryService.setGroup(request);
		if (response == null || !response.isSuccess()) {
			log.warn("The AD group could not be set!");
		}
	}

	@Transactional
	public void deleteGroupMembership(Employee employee, long groupId) {
		AdUser user = employee.getUser();
		if (user == null) {
			log.warn("The employee: {} does not have an active directory account!", employee.getName());
			return;
		}
		AdGroup adGroup = adGroupService.findById(groupId);
		if (adGroup == null) {
			log.warn("The ad Group with id: {} could not be found!", groupId);
			return;
		}

		RemoveGroupRequest request = new RemoveGroupRequest();
		request.setGroup(adGroup.getName());
		request.setUserId(user.getName());

		RemoveGroupResponse removeGroupResponse = activeDirectoryService.removeGroup(request);
		if (!removeGroupResponse.isSuccess()) {
			log.warn("The AD group could not be removed! " + removeGroupResponse.getMessage());
			return;
		}

//		user.removeAdGroup(adGroup);
		adUserService.save(user);
	}

	public void setExpireDate(Employee employee, LocalDate stopDate) {

		AdUser user = employee.getUser();
		if (user == null) {
			log.warn("The employee: {} does not have an active directory account!", employee.getName());
			return;
		}
		SetExpireRequest expireRequest = new SetExpireRequest();
		expireRequest.setUserId(user.getName());
		expireRequest.setTts(stopDate.format(DATE_TIME_FORMATTER));

		SetExpireResponse setExpireResponse = activeDirectoryService.setExpire(expireRequest);

		if (!setExpireResponse.isSuccess()) {
			log.warn("The expire date could not be set! " + setExpireResponse.getMessage());
		}
	}

	public void setPassword(Employee employee, String password) {
		AdUser user = employee.getUser();
		if (user == null) {
			log.warn("The employee: {} does not have an active directory account!", employee.getName());
			return;
		}
		SetPasswordRequest request = new SetPasswordRequest();
		request.setUserId(user.getName());
		request.setPassword(password);

		SetPasswordResponse response = activeDirectoryService.setPassword(request);
		if (!response.getStatus().equals(SetPasswordResponse.PasswordStatus.OK)) {
			log.warn("The password could not be set! " + response.getMessage());
		}
	}

	public void forcePasswordChange(Employee employee) {
		AdUser user = employee.getUser();
		if (user == null) {
			log.warn("The employee: {} does not have an active directory account!", employee.getName());
			return;
		}
		ForcePasswordChangeRequest request = new ForcePasswordChangeRequest();
		request.setUserId(user.getName());

		ForcePasswordChangeResponse response = activeDirectoryService.setForcePasswordChange(request);
		if (!response.isSuccess()) {
			log.warn("The password could not be set! " + response.getMessage());
		}
	}

	private String makeAdUserName(Employee employee) {
		// Compute the prefix which includes the configured global prefix and the placeholder that each Business has.
		String basePrefix = configuration.getActiveDirectoryConfig().getPrefix();
		String usernamePrefix = basePrefix.replace("PLACEHOLDER_BUSINESS_PREFIX", employee.getBusiness().getPrefix());

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
