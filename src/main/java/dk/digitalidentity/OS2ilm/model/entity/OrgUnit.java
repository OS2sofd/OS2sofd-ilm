package dk.digitalidentity.OS2ilm.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.util.HashSet;
import java.util.Set;

@Audited
@Getter
@Setter
@Entity(name = "orgunit")
public class OrgUnit {

	@Id
	@Column
	private String uuid;

	@Column
	private String name;

	@Column
	private String parentUuid;

	// TODO: I do not know if this is needed - might be an unnecessary bi-directional relationship
	@OneToMany(mappedBy = "orgUnit", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JsonIgnore
	@NotAudited
	private Set<Employee> employees = new HashSet<>();

	// TODO: I do not know if this is needed - might be an unnecessary bi-directional relationship
	@OneToMany(mappedBy = "defaultOrgUnit", cascade = {CascadeType.PERSIST, CascadeType.MERGE })
	@JsonIgnore
	@NotAudited
	private Set<Business> businesses = new HashSet<>();
}
