package dk.digitalidentity.OS2ilm.model.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OUTreeForm {
	private String id;
	private String parent;
	private String text;

	public OUTreeForm(OrgUnit ou) {
		this.text = ou.getName();
		this.id = ou.getUuid();
		this.parent = (ou.getParentUuid() != null) ? ou.getParentUuid() : "#";
	}

	public OUTreeForm() {}
}
