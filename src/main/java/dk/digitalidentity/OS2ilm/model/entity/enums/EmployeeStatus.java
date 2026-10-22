package dk.digitalidentity.OS2ilm.model.entity.enums;

import lombok.Getter;

@Getter
public enum EmployeeStatus {
	AWAITING_FULL_INTEGRATION("Afventer at blive integreret"),
	AWAITING_RC_INTEGRATION("er integreret i Sofd"),
	FULLY_INTEGRATED("Fuldt integreret");

	private String displayName;

	private EmployeeStatus(String displayName) {
		this.displayName = displayName;
	}
}
