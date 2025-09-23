package dk.digitalidentity.OS2ilm.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "user_role")
public class UserRole {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private long userRoleId;

	@Column(nullable = false, length = 64)
	private String name;

	@Column
	private String description;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "it_system_id")
	private ItSystem itSystem;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UserRole userRole = (UserRole) o;
		return id == userRole.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
