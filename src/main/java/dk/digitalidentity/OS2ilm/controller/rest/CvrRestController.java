package dk.digitalidentity.OS2ilm.controller.rest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.OS2ilm.model.role.NoRoleRequired;
import dk.digitalidentity.OS2ilm.service.CvrService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;

@Hidden
@RestController
@NoRoleRequired
@RequiredArgsConstructor
public class CvrRestController {

	private final CvrService cvrService;

	@GetMapping("/rest/search/{cvr}")
	public ResponseEntity<?> getCvr(@PathVariable("cvr") String cvr) {
		return new ResponseEntity<>(cvrService.getSearchResultByCvr(cvr), HttpStatus.OK);
	}
}
