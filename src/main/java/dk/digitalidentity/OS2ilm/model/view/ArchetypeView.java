package dk.digitalidentity.OS2ilm.model.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

@Getter
@Entity(name = "view_archetype")
public class ArchetypeView {

	@Id
	@Column
	@JsonProperty("id")
	private long id;

	@Column
	@JsonProperty("name")
	private String name;
}
