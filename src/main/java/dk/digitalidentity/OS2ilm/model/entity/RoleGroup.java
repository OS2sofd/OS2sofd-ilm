package dk.digitalidentity.OS2ilm.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.Objects;

@Audited
@Entity
@Getter
@Setter
@Table(name = "role_group")
public class RoleGroup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private long roleGroupId;

	@Column(nullable = false, length = 64)
	private String name;

	@Column
	private String description;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RoleGroup roleGroup = (RoleGroup) o;
		return id == roleGroup.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
