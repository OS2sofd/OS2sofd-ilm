package dk.digitalidentity.OS2ilm.service.eboks;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EboksMessage {
	private String cpr;
	private String subject;
	private String content;
	private String cvr;
	private String municipalityName;
}
