package dk.digitalidentity.OS2ilm.model.entity;

import dk.digitalidentity.OS2ilm.model.entity.enums.ApiRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "client")
public class Client {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

	@NotNull
	@Column
	private String apiKey;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column
	private ApiRole role;

	@Column
	private String description;

	@Column
	private boolean enabled;
}
