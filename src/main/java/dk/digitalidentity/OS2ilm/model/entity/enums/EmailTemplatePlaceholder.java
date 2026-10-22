package dk.digitalidentity.OS2ilm.model.entity.enums;

import lombok.Getter;

@Getter
public enum EmailTemplatePlaceholder {
	RECIPIENT("{modtager}", "Navnet på modtageren af beskeden", "Hans Hansen"),
	USERNAME("{brugernavn}", "Brugernavnet på medarbejderens AD-konto", "abc123"),
	PASSWORD("{kodeord}", "Kodeordet til medarbejderens AD-konto", "Sommer-Bil-42"),
	DATE("{dato}", "Den dato medarbejderens AD-konto er forlænget til", "2026-12-31"),
	COUNT("{antal}", "Antal medarbejdere der afventer godkendelse", "3"),
	LINK("{link}", "Link til den relevante side i OS2ILM", "https://os2ilm.kommune.dk/approve");

	private final String placeholder;
	private final String description;
	private final String example;

	EmailTemplatePlaceholder(String placeholder, String description, String example) {
		this.placeholder = placeholder;
		this.description = description;
		this.example = example;
	}
}
