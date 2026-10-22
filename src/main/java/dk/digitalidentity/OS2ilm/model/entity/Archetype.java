package dk.digitalidentity.OS2ilm.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Audited
@Getter
@Setter
@Entity(name = "archetype")
public class Archetype {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String name;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "archetype_user_role_mapping",
			joinColumns = @JoinColumn(name = "archetype_id"),
			inverseJoinColumns = @JoinColumn(name = "user_role_id")
	)
	@Audited
	private Set<UserRole> userRoles = new HashSet<>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "archetype_ad_group_mapping",
			joinColumns = @JoinColumn(name = "archetype_id"),
			inverseJoinColumns = @JoinColumn(name = "ad_group_id")
	)
	@Audited
	private Set<AdGroup> adGroups = new HashSet<>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "archetype_role_group_mapping",
			joinColumns = @JoinColumn(name = "archetype_id"),
			inverseJoinColumns = @JoinColumn(name = "role_group_id")
	)
	@Audited
	private Set<RoleGroup> roleGroups = new HashSet<>();

	// Helper methods to maintain the relationships properly
	public void addUserRole(UserRole userRole) {
		userRoles.add(userRole);
	}

	public void removeUserRole(UserRole userRole) {
		userRoles.remove(userRole);
	}

	public void addAdGroup(AdGroup adGroup) {
		adGroups.add(adGroup);
	}

	public void removeAdGroup(AdGroup adGroup) {
		adGroups.remove(adGroup);
	}

	public void addRoleGroup(RoleGroup roleGroup) {
		roleGroups.add(roleGroup);
	}

	public void removeRoleGroup(RoleGroup roleGroup) {
		roleGroups.remove(roleGroup);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Archetype archetype = (Archetype) o;
		return id == archetype.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
