package dk.digitalidentity.OS2ilm.model.entity.enums;

import lombok.Getter;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Getter
public enum LogAction {
    // Enums for log actions
    LOGIN(ActionType.LOGIN, ReportType.LOGIN_HISTORY),
    ADDED_CMS_MESSAGE(ActionType.CMS, ReportType.ADMIN_ACTION),
    DELETED_CMS_MESSAGE(ActionType.CMS, ReportType.ADMIN_ACTION),
    ACTIVATED_CMS_MESSAGE(ActionType.CMS, ReportType.ADMIN_ACTION),
	EMAIL_TEMPLATE_UPDATED(ActionType.UPDATED, ReportType.ADMIN_ACTION),
	EMAIL_TEMPLATE_ACTIVATED(ActionType.ACTIVATED, ReportType.ADMIN_ACTION),
	EMAIL_TEMPLATE_DEACTIVATED(ActionType.UPDATED, ReportType.ADMIN_ACTION),
	REGISTRATION(ActionType.REGISTERED, ReportType.NONE),
	AD_USER_CREATED(ActionType.CREATED, ReportType.SYSTEM_ACTION),
	AD_USER_DISABLED(ActionType.UPDATED, ReportType.SYSTEM_ACTION),
	AD_USER_DELETED(ActionType.DELETED, ReportType.SYSTEM_ACTION),
	EMPLOYEE_DENIED(ActionType.DENIED, ReportType.ADMIN_ACTION),
	EMPLOYEE_ADMIN_APPROVED(ActionType.APPROVED, ReportType.ADMIN_ACTION),
	EMPLOYEE_LEADER_APPROVED(ActionType.APPROVED, ReportType.ADMIN_ACTION),
	ARCHETYPE_DELETED(ActionType.DELETED, ReportType.ADMIN_ACTION),
	ARCHETYPE_CREATED(ActionType.CREATED, ReportType.ADMIN_ACTION),
	ARCHETYPE_UPDATED(ActionType.UPDATED, ReportType.ADMIN_ACTION),
	BUSINESS_DELETED(ActionType.DELETED, ReportType.ADMIN_ACTION),
	BUSINESS_CREATED(ActionType.CREATED, ReportType.ADMIN_ACTION),
	BUSINESS_UPDATED(ActionType.UPDATED, ReportType.ADMIN_ACTION),
	REQUESTED_EXTENSION(ActionType.REQUEST, ReportType.NONE),
	UPDATED_PROFILE(ActionType.UPDATED, ReportType.NONE),
	RESET_PASSWORD(ActionType.RESET, ReportType.NONE),
	EMPLOYEE_UPDATED(ActionType.UPDATED, ReportType.ADMIN_ACTION),
	EMPLOYEE_CREATED(ActionType.CREATED, ReportType.ADMIN_ACTION),
	EMPLOYEE_DELETED(ActionType.DELETED, ReportType.ADMIN_ACTION),
	EMPLOYEE_CLOSED(ActionType.CLOSED, ReportType.ADMIN_ACTION),
	EMPLOYEE_REOPENED(ActionType.REOPENED, ReportType.ADMIN_ACTION),
	EXCHANGE_ORDER_REQUESTED(ActionType.REQUEST, ReportType.ADMIN_ACTION),
	EXTENSION_APPROVED(ActionType.APPROVED, ReportType.ADMIN_ACTION),
	EXTENSION_DENIED(ActionType.DENIED, ReportType.ADMIN_ACTION),
	ACTIVATION_TIME_FRAME_CREATED(ActionType.CREATED, ReportType.ADMIN_ACTION),
	ACTIVATION_TIME_FRAME_UPDATED(ActionType.UPDATED, ReportType.ADMIN_ACTION),
	ACTIVATION_TIME_FRAME_DELETED(ActionType.DELETED, ReportType.ADMIN_ACTION),
	LEADER_CREATED(ActionType.CREATED, ReportType.ADMIN_ACTION),
	LEADER_UPDATED(ActionType.UPDATED, ReportType.ADMIN_ACTION),
	LEADER_DELETED(ActionType.DELETED, ReportType.ADMIN_ACTION),
	QUEUE_FAILED(ActionType.QUEUED, ReportType.SYSTEM_ACTION);

    private ActionType type;
    private ReportType[] reportTypes;
    private boolean includeInList = true;

    LogAction(ActionType type, ReportType... reportTypes) {
        this.type = type;
        this.reportTypes = reportTypes;
    }

    public record LogActionDTO(LogAction logAction, String message) { }

    public static List<LogActionDTO> getSorted(ResourceBundleMessageSource resourceBundle, Locale locale) {
        List<LogActionDTO> dtos = new ArrayList<>();

        for (LogAction logAction : LogAction.values()) {
            if (logAction.isIncludeInList()) {
                String newMessage = resourceBundle.getMessage(logAction.getType().getTranslation(), null, locale);
                LogActionDTO dto = new LogActionDTO(logAction, newMessage);

                dtos.add(dto);
            }
        }

        dtos.sort((a, b) -> a.message().compareToIgnoreCase(b.message()));

        return dtos;
    }

    public enum ReportType { ALL, NONE, GENERAL_HISTORY, LOGIN_HISTORY, ADMIN_ACTION, SYSTEM_ACTION }
	public enum ActionType {
		CREATED("Oprettet"),
		UPDATED("Opdateret"),
		DELETED("Slettet"),
		CMS("CMS"),
		REGISTERED("Registreret"),
		DENIED("Afvist"),
		APPROVED("Godkendt"),
		ACTIVATED("Aktiveret"),
		REOPENED("Genåbnet"),
		CLOSED("Lukket"),
		REQUEST("Anmodning"),
		RESET("Nulstillet"),
		LOGIN("Login"),
		QUEUED("SYSTEM FEJL");

		private final String translation;

		ActionType(String translation) {
			this.translation = translation;
		}

		public String getTranslation() {
			return translation;
		}
	}
}
