package dk.digitalidentity.OS2ilm.model.entity;

import dk.digitalidentity.OS2ilm.model.entity.enums.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity(name = "ad_user")
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

//	@ManyToMany(fetch = FetchType.EAGER)
//	@JoinTable(
//			name = "ad_user_ad_group_mapping",
//			joinColumns = @JoinColumn(name = "ad_user_id"),
//			inverseJoinColumns = @JoinColumn(name = "ad_group_id")
//	)
//	private Set<AdGroup> adGroups = new HashSet<>();

	@OneToOne(mappedBy = "user")
	private Employee employee;

	@Enumerated(EnumType.STRING)
	@Column
	public AccountStatus status;

	@ManyToOne()
	@JoinColumn(name = "activation_time_frame_id")
	private ActivationTimeFrame activationTimeFrame;

	@Column
	private boolean enabled;

	@Column
	private String extUuid;

//	public void addAdGroup(AdGroup adGroup) {
//		System.out.println("Adding ad group: " + adGroup.getName() + " to user: " + name);
//		adGroups.add(adGroup);
//	}
//
//	public void removeAdGroup(AdGroup adGroup) {
//		adGroups.remove(adGroup);
//	}
//
//	public String getAdUserNames() {
//		StringBuilder sb = new StringBuilder();
//		for (AdGroup adGroup : adGroups) {
//			if (!sb.isEmpty()) {
//				sb.append(adGroup.getName()).append(",");
//			} else {
//				sb.append(adGroup.getName());
//			}
//		}
//		return sb.toString();
//	}

}
