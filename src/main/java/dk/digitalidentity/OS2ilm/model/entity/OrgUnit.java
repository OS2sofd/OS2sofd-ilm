package dk.digitalidentity.OS2ilm.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

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

	@OneToMany(mappedBy = "orgUnit", cascade = CascadeType.ALL)
	@JsonIgnore
	private Set<Employee> employees = new HashSet<>();
}
