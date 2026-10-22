package dk.digitalidentity.OS2ilm.model.entity.enums;

import static dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplatePlaceholder.COUNT;
import static dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplatePlaceholder.DATE;
import static dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplatePlaceholder.LINK;
import static dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplatePlaceholder.PASSWORD;
import static dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplatePlaceholder.RECIPIENT;
import static dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplatePlaceholder.USERNAME;

import java.util.List;

import lombok.Getter;

@Getter
public enum EmailTemplateType {
	EMPLOYEE_APPROVED(
			"Medarbejder godkendt",
			"Sendes via e-boks/email når en medarbejder er blevet godkendt og har fået oprettet sin AD-konto.",
			true,
			List.of(RECIPIENT, USERNAME, PASSWORD),
			"OS2ILM Anmodning godkendt",
			"Kære {modtager} <br> Du er nu blevet godkendt i OS2ILM. Din AD-Konti login er: <br>{brugernavn} <br>{kodeord}"),
	EMPLOYEE_CREATED(
			"Medarbejder oprettet",
			"Sendes via e-boks/email når en medarbejder er blevet oprettet i løsningen.",
			true,
			List.of(RECIPIENT),
			"OS2ILM Ny adgang",
			"Kære {modtager} <br> Du er nu blevet oprettet i OS2ILM. Husk at du skal acceptere vilkår inden brug"),
	EMPLOYEE_EXTENDED(
			"AD-konto forlænget",
			"Sendes via e-boks/email når en medarbejder har fået sin AD-konto forlænget.",
			true,
			List.of(RECIPIENT, DATE),
			"OS2ILM Adgang forlænget",
			"Kære {modtager} <br> Din AD-Konti er blevet forlænget indtil: {dato}"),
	EMPLOYEE_CLOSED(
			"AD-konto lukket",
			"Sendes via e-boks/email når en medarbejder har fået sin AD-konto lukket.",
			true,
			List.of(RECIPIENT),
			"OS2ILM Adgang lukket",
			"Kære {modtager} <br> Din AD-Konti er blevet låst."),
	EMPLOYEE_UNLOCKED(
			"AD-konto genåbnet",
			"Sendes via e-boks/email når en medarbejder har fået sin AD-konto genåbnet.",
			true,
			List.of(RECIPIENT),
			"OS2ILM Adgang genåbnet",
			"Kære {modtager} <br> Din AD-Konti er blevet genåbnet."),
	EMPLOYEE_PASSWORD_RESET(
			"Kodeord nulstillet",
			"Sendes via e-boks/email når en medarbejder har fået sit AD-kodeord nulstillet.",
			true,
			List.of(RECIPIENT, PASSWORD),
			"OS2ILM Kodeord nulstillet",
			"Kære {modtager} <br> Kodeordet på din AD-Konti er blevet nulstillet. Dit kodeord er nu følgende: <br> {kodeord}"),
	EMPLOYEE_CREATE_REQUEST(
			"Anmodning om godkendelse",
			"Sendes via email til lederen når en medarbejder har registreret sig og anmoder om godkendelse.",
			false,
			List.of(),
			"OS2ILM Anmodning om godkendelse",
			"En medarbejder har anmodet om godkendelse. Du skal logge ind i OS2ILM og godkende medarbejderen."),
	ADMIN_APPROVED(
			"Godkendt af administrator",
			"Sendes via email til lederne når en administrator har godkendt en medarbejder, som ikke blev godkendt af en leder.",
			false,
			List.of(RECIPIENT),
			"OS2ILM Medarbejder godkendt af administrator",
			"Kære {modtager} <br> En administrator har godkendt en af dine medarbejder i OS2ILM."),
	MAIL_INVITATION(
			"Invitation",
			"Sendes via email når en administrator inviterer en konsulent til OS2ILM.",
			false,
			List.of(RECIPIENT, LINK),
			"OS2ILM: Du er blevet inviteret til OS2ILM",
			"Kære {modtager} <br> Du har fået en invitation til at registrere dig i OS2ILM. Hvis du følger linket nedenfor, kan du læse om hvordan du nemt kan registrere dig. <br> <br> Registreringsside: {link}"),
	AWAITING_APPROVAL(
			"Påmindelse om godkendelser",
			"Sendes via email til ledere/administrator på et fast interval, når der er medarbejdere der afventer godkendelse.",
			false,
			List.of(RECIPIENT, COUNT, LINK),
			"OS2ILM: Nye medarbejder godkendelser",
			"Kære {modtager} <br> Du har {antal} brugere der skal godkendes eller afvises i OS2ILM. <br> <br> Godkendelsesside: {link}");

	private final String title;
	private final String description;

	// templates that can be sent via Digital Post, which restricts which characters the subject may contain
	private final boolean digitalPost;
	private final List<EmailTemplatePlaceholder> placeholders;
	private final String defaultSubject;
	private final String defaultContent;

	EmailTemplateType(String title, String description, boolean digitalPost, List<EmailTemplatePlaceholder> placeholders, String defaultSubject, String defaultContent) {
		this.title = title;
		this.description = description;
		this.digitalPost = digitalPost;
		this.placeholders = placeholders;
		this.defaultSubject = defaultSubject;
		this.defaultContent = defaultContent;
	}
}
