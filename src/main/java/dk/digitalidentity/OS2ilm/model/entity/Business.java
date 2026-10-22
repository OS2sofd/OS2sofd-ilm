package dk.digitalidentity.OS2ilm.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Audited
@Getter
@Setter
@Entity(name = "business")
public class Business {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

	@Column
	private String name;

	@Column(unique = true)
	private String cvr;

	@Column
	private String prefix;

	@OneToMany(mappedBy = "business")
	@JsonIgnore
	private List<Employee> employees = new ArrayList<>();

	@ManyToOne
	@JoinColumn(name = "activation_time_frame_id")
	private ActivationTimeFrame activationTimeFrame;

	@ManyToOne
	@JoinColumn(name = "default_org_unit_uuid")
	private OrgUnit defaultOrgUnit;

	@Column
	private boolean enableLeaders;

	@Column
	private boolean inheritPrivileges;

	@NotAudited
	@OneToOne(mappedBy = "business", cascade = CascadeType.ALL)
	private BusinessEmploymentSettings businessEmploymentSettings;

	@ManyToMany
	@JoinTable(
			name = "business_archetypes",
			joinColumns = @JoinColumn(name = "business_id"),
			inverseJoinColumns = @JoinColumn(name = "archetype_id")
	)
	private Set<Archetype> selectableArchetypes = new HashSet<>();

	@ManyToMany(mappedBy = "businesses")
	@JsonIgnore
	private Set<Leader> leaders = new HashSet<>();

	public void addSelectableArchetype(Archetype archetype) {
		selectableArchetypes.add(archetype);
	}

	public void removeSelectableArchetype(Archetype archetype) {
		selectableArchetypes.remove(archetype);
	}
}
