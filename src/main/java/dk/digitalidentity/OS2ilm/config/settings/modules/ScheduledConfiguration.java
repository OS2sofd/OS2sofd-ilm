package dk.digitalidentity.OS2ilm.config.settings.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduledConfiguration {
	private boolean enabled = true;

	public boolean cleanUpAuditLogEnabled = true;
	public String cleanUpAuditLogCron;
	public int cleanUpInterval = 13;

	public boolean updateActiveDirectoryEnabled = true;
	public String updateActiveDirectoryCron;

	public boolean accountActiveDirectoryEnabled = true;
	public String accountActiveDirectoryCron;

	public String sofdSyncCron;

	public String rcSyncCron;
	public String rcAssignCron;

	public boolean apiKeyGenerationEnabled = true;

	public String cleanUpOldUsersCron;
	public boolean cleanUpOldUsersEnabled = true;
	// Representing the number of months before we start deleting employees
	public int cleanUpOldUsersInterval = 13;
}
