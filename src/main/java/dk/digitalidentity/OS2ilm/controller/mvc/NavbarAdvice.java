package dk.digitalidentity.OS2ilm.controller.mvc;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@RequiredArgsConstructor
@ControllerAdvice(basePackages = "dk.digitalidentity.OS2ilm.controller.mvc")
public class NavbarAdvice {

	private final IlmConfiguration ilmConfiguration;

	@ModelAttribute
	public void addAppVersion(Model model) {
		model.addAttribute("appVersion", ilmConfiguration.getVersion());
		model.addAttribute("latestVersion", ilmConfiguration.getLatestVersion());
	}
}
