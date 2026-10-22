package dk.digitalidentity.OS2ilm.model.entity;

import java.time.LocalDateTime;

import dk.digitalidentity.OS2ilm.model.entity.enums.EmailTemplateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class EmailTemplate {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	@Enumerated(EnumType.STRING)
	private EmailTemplateType templateType;

	@Column
	private String subject;

	@Column
	private String content;

	@Column
	private boolean enabled = true;

	@Column
	private String s3AttachmentKey;

	@Column
	private LocalDateTime lastUpdated;
}
