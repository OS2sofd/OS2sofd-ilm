package dk.digitalidentity.OS2ilm.model.entity;

import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmployeeStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Entity(name = "employee")
public class Employee {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

	@Column
	private String name;

	@Column
	private String mitIdUuid;

	@Column
	private String cpr;

	@Column
	private String email;

	@Column
	private String phone;

	@ManyToOne
	@JoinColumn(name = "business_id")
	private Business business;

	@Column
	@Enumerated(EnumType.STRING)
	private ApproveStatus approved;

	@Column
	@Enumerated(EnumType.STRING)
	private EmployeeStatus integrated;

	@Column
	private String cvr;

	@Column
	private String positionName;

	@Column
	private LocalDate startDate;

	@Column
	private LocalDate stopDate;

	@Column
	private boolean acceptedTos;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "ad_user_id")
	private AdUser user;

	@Column
	private boolean blocked;

	// Needed for ad creation after tos has been accepted
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "activation_time_frame_id")
	private ActivationTimeFrame activationTimeFrame;

	@Column
	private String role;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "org_unit_uuid")
	private OrgUnit orgUnit;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "employee_ad_group_mapping",
			joinColumns = @JoinColumn(name = "employee_id"),
			inverseJoinColumns = @JoinColumn(name = "ad_group_id")
	)
	private Set<AdGroup> adGroups = new HashSet<>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "employee_user_role_mapping",
			joinColumns = @JoinColumn(name = "employee_id"),
			inverseJoinColumns = @JoinColumn(name = "user_role_id")
	)
	private Set<UserRole> userRoles = new HashSet<>();

	public void addUserRole(UserRole userRole) {
		userRoles.add(userRole);
	}

	public void addAdGroup(AdGroup adGroup) {
		adGroups.add(adGroup);
	}

	@Column
	private LocalDateTime lastLogin;

	@Column
	private String extUuid;
}
