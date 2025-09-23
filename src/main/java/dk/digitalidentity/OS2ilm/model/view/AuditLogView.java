package dk.digitalidentity.OS2ilm.model.view;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity(name = "view_audit_log")
public class AuditLogView {

	@Id
	@Column
	@JsonProperty("id")
	private long id;

	@Column
	@JsonProperty("tts")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
	private LocalDateTime tts;

	@Column
	@JsonProperty("correlationId")
	private String correlationId;

	@Column
	@JsonProperty("targetId")
	private String targetId;

	@Column
	@JsonProperty("targetName")
	private String targetName;

	@Column
	@JsonProperty("performerId")
	private String performerId;

	@Column
	@JsonProperty("performerName")
	private String performerName;

	@Column
	@JsonProperty("message")
	private String message;

	@Column
	@JsonProperty("logAction")
	@Enumerated(EnumType.STRING)
	private LogAction logAction;

	@JoinColumn
	@JsonProperty("detailId")
	private Long detailId;
}
