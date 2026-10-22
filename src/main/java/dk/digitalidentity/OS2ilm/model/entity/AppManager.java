package dk.digitalidentity.OS2ilm.model.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppManager {
	private String url = "https://appmanager.digital-identity.dk/api";

	public String getUrl() {
		return url.endsWith("/") ? url : url + "/";
	}
}
