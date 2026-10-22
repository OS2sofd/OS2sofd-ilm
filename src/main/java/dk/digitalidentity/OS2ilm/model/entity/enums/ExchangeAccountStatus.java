package dk.digitalidentity.OS2ilm.model.entity.enums;

public enum ExchangeAccountStatus {
	// No Exchange account and none requested
	NONE,
	// Marked for ordering - picked up by the scheduled SOFD ordering task
	NEEDS_ORDER,
	// An Exchange account has been ordered / exists for this employee
	HAS_ACCOUNT,
	// The order failed - can be reordered
	ORDER_FAILED
}
