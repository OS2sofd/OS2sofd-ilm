package dk.digitalidentity.OS2ilm.model.entity.enums;

public enum AccountNameStrategy {
	PREFIXED,	// Legacy prefix: DI-XXX
	TAILORED,	// Multiple snippets put into one
	SOFD,	// SOFD handles it all
	CUSTOM	// The approver handles the FULL name
}
