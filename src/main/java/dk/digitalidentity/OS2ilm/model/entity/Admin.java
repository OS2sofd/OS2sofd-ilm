package dk.digitalidentity.OS2ilm.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "admin")
public class Admin {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

	@Column
	private String name;

	@Column(nullable = false, unique = true)
	private String uuid;

	@Column
	private String email;

	@Column
	private String role;
}
