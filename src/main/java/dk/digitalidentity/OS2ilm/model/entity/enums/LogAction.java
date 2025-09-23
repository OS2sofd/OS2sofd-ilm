package dk.digitalidentity.OS2ilm.model.entity.enums;

import lombok.Getter;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Getter
public enum LogAction {
    // for the small report (the one for 3 months, due to huge amounts of log-data)
    LOGIN("Login gennemført", ReportType.LOGIN_HISTORY),
    ADDED_CMS_MESSAGE("CMS tekst tilføjet", ReportType.ADMIN_ACTION),
    DELETED_CMS_MESSAGE("CMS tekst slettet", ReportType.ADMIN_ACTION),
    AUDITLOG_CLEANUP("Oprydning af gamle auditlogs gennemført", ReportType.NONE),
	BUSINESS_REGISTRATION("Ny virksomhed oprettet under registrering", ReportType.SYSTEM_ACTION),
	REGISTRATION("Ny bruger har registreret sig i løsningen", ReportType.NONE),
	AD_USER_CREATED("Ny bruger oprettet i AD", ReportType.SYSTEM_ACTION),
	ACTIVATION_TIME_FRAME_DELETED("Aktivitetsperiode slettet", ReportType.ADMIN_ACTION),
	EMPLOYEE_DENIED("Medarbejder er blevet afvist", ReportType.ADMIN_ACTION),
	EMPLOYEE_ADMIN_APPROVED("Medarbejder er blevet godkendt af en administrator", ReportType.ADMIN_ACTION),
	EMPLOYEE_LEADER_APPROVED("Medarbejder er blevet godkendt af en leder", ReportType.ADMIN_ACTION),
	ARCHETYPE_DELETED("Arketypen er blevet slettet", ReportType.ADMIN_ACTION),
	ARCHETYPE_CREATED("Ny arketype blev oprettet", ReportType.ADMIN_ACTION),
	ARCHETYPE_UPDATED("Arketypen er blevet opdateret", ReportType.ADMIN_ACTION),
	BUSINESS_DELETED("Virksomheden er blevet slettet", ReportType.ADMIN_ACTION),
	BUSINESS_CREATED("Ny virksomhed blev oprettet", ReportType.ADMIN_ACTION),
	BUSINESS_UPDATED("Virksomheden er blevet opdateret", ReportType.ADMIN_ACTION),
	REQUESTED_EXTENSION("Medarbejder har anmodet om forlængelse", ReportType.NONE),
	UPDATED_PROFILE("Medarbejder har opdateret sit profil", ReportType.NONE),
	RESET_PASSWORD("Medarbejder har nulstillet deres kodeord", ReportType.NONE),
	EMPLOYEE_UPDATED("Medarbejder blev opdateret", ReportType.ADMIN_ACTION),
	EMPLOYEE_CREATED("En ny medarbejder blev oprettet", ReportType.ADMIN_ACTION),
	EMPLOYEE_DELETED("Medarbejderen blev slettet", ReportType.ADMIN_ACTION),
	EMPLOYEE_CLOSED("Medarbejderen blev lukket", ReportType.ADMIN_ACTION),
	EMPLOYEE_REOPENED("Medarbejderen blev oplukket igen", ReportType.ADMIN_ACTION),
	EXTENSION_APPROVED("Medarbejderens forlængelse blev godkendt", ReportType.ADMIN_ACTION),
	EXTENSION_DENIED("Medarbejderens forlængelse blev afvist", ReportType.ADMIN_ACTION),
	ACTIVATION_TIME_FRAME_CREATED("Aktiveringsperiode blev oprettet", ReportType.ADMIN_ACTION),
	ACTIVATION_TIME_FRAME_UPDATED("Aktiveringsperiode blev opdateret", ReportType.ADMIN_ACTION);


    private String message;
    private ReportType[] reportTypes;
    private boolean includeInList = true;

    private LogAction(String message, ReportType... reportTypes) {
        this.message = message;
        this.reportTypes = reportTypes;
    }

    public record LogActionDTO(LogAction logAction, String message) { }

    public static List<LogActionDTO> getSorted(ResourceBundleMessageSource resourceBundle, Locale locale) {
        List<LogActionDTO> dtos = new ArrayList<>();

        for (LogAction logAction : LogAction.values()) {
            if (logAction.isIncludeInList()) {
                String newMessage = resourceBundle.getMessage(logAction.getMessage(), null, locale);
                LogActionDTO dto = new LogActionDTO(logAction, newMessage);

                dtos.add(dto);
            }
        }

        dtos.sort((a, b) -> a.message().compareToIgnoreCase(b.message()));

        return dtos;
    }

    public enum ReportType { ALL, NONE, GENERAL_HISTORY, LOGIN_HISTORY, ADMIN_ACTION, SYSTEM_ACTION }
}
