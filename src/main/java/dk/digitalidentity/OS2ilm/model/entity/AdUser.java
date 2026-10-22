package dk.digitalidentity.OS2ilm.model.entity;

import dk.digitalidentity.OS2ilm.model.entity.enums.AccountStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.SofdSyncStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.time.LocalDate;

@Audited
@Getter
@Setter
@Entity(name = "ad_user")
@SQLRestriction("deleted = false")
public class AdUser {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	// sAMAccountName
	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private LocalDate startDate;

	@Column(nullable = false)
	private LocalDate stopDate;

	@OneToOne(mappedBy = "user")
	private Employee employee;

	@Enumerated(EnumType.STRING)
	@Column
	public AccountStatus status;

	@NotAudited
	@ManyToOne()
	@JoinColumn(name = "activation_time_frame_id")
	private ActivationTimeFrame activationTimeFrame;

	@Column
	private boolean enabled;

	@Column
	private String extUuid;

	@Column
	private String cpr;

	@Column
	private boolean initialPasswordSet;

	@NotAudited
	@Enumerated(EnumType.STRING)
	@Column(name = "sofd_sync_status")
	private SofdSyncStatus sofdSyncStatus;

	@Column
	private boolean deleted = false;

	@Column
	private String phoneNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "business_id")
	private Business business;
}
