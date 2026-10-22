package dk.digitalidentity.OS2ilm.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.HashSet;
import java.util.Set;

@Audited
@Getter
@Setter
@Entity(name = "leader")
public class Leader {
	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String mitIdUuid;

	@Column
	private String cpr;

	@Column
	private String internalUuid;

	@Column
	private String name;

	@Column
	private String mail;

	@ManyToMany
	@JoinTable(
			name = "leader_business_mapping",
			joinColumns = @JoinColumn(name = "leader_id"),
			inverseJoinColumns = @JoinColumn(name = "business_id")
	)
	private Set<Business> businesses = new HashSet<>();

	@Transient
	public String getIdentifier() {
		if (mitIdUuid != null) {
			return mitIdUuid;
		}
		if (internalUuid != null) {
			return internalUuid;
		}
		return cpr;
	}
}
