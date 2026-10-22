package dk.digitalidentity.OS2ilm.service.eboks;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class EboksMessage {
	private String cpr;
	private String subject;
	private String content;
	private String cvr;
	private String municipalityName;

	// same format as the email library's e-boks message - always sent as a list, also when empty
	private List<EboksAttachment> attachments = new ArrayList<>();

	public record EboksAttachment(String filename, String content) { }
}
