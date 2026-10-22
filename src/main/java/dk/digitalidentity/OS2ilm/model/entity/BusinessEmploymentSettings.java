package dk.digitalidentity.OS2ilm.model.entity;

import dk.digitalidentity.OS2ilm.model.entity.enums.AccountNameStrategy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Audited
@Getter
@Setter
@Entity(name = "business_employment_settings")
public class BusinessEmploymentSettings {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column
	private long id;

	@OneToOne
	@JoinColumn(name = "business_id")
	private Business business;

	@Column
	private Integer maxFutureDays;

	@Column
	private Integer maxEmploymentDays;

	@Column
	private Integer defaultEmploymentDays;

	@Column
	private boolean sofdGeneratedName;

	@Column
	private boolean enableOrdering;

	@CreationTimestamp
	private LocalDateTime createdAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;

	@Column
	@Enumerated(EnumType.STRING)
	private AccountNameStrategy nameStrategy;

	@Column
	private String accountNameTemplate;
}
