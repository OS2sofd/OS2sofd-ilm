package dk.digitalidentity.OS2ilm.model.entity.enums;

public enum AccountStatus {
	AVAILABLE("Ledig"),
	RESERVED("Reserveret"),
	IN_USE("I Brug");

	private String displayName;

	private AccountStatus(String displayName) {
		this.displayName = displayName;
	}
}
