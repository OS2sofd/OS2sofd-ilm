package dk.digitalidentity.OS2ilm.controller.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleOrgUnit {
	private String name;
	private String uuid;
	private String parentUuid;
	private Boolean deleted;
}
