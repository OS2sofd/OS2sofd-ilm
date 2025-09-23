package dk.digitalidentity.OS2ilm.controller.api.model;

import lombok.Data;

@Data
public class GetBusinessResult {
	private Virksomhed virksomhed;
	private VirksomhedsNavn virksomhedsnavn;
}
