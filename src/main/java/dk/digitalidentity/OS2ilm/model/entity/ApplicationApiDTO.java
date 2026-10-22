package dk.digitalidentity.OS2ilm.model.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationApiDTO {
	private String identifier;
	private String name;
	private String deploymentType;
	private String newestVersion;
	private String minimumVersion;
}
