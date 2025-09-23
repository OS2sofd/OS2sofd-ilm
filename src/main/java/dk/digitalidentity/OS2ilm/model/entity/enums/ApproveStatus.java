package dk.digitalidentity.OS2ilm.model.entity.enums;

public enum ApproveStatus {
	ADMIN_APPROVED("Admin har Godkendt"),
	LEADER_APPROVED("Leder har Godkendt"),
	AWAITING_APPROVAL("Afventer godkendelse"),
	AWAITING_EXTENSION_APPROVAL("Afventer godkendelse om forlængelse"),
	DENIED("Afvist");

	private String displayName;

	private ApproveStatus(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
