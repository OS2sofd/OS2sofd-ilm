package dk.digitalidentity.OS2ilm.controller.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Virksomhed {
	@JsonProperty("CVRNummer")
	private String cvr;
}
