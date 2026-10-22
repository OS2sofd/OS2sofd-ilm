package dk.digitalidentity.OS2ilm.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.service.BusinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RequestMapping("/api/business")
@Tag(name = "Business API", description = "Endpoints for managing businesses")
@RequiredArgsConstructor
@RestController
public class BusinessApiController {
	private final BusinessService businessService;

	@Operation(
			summary = "Output businesses",
			description = "Outputs a list of all businesses",
			responses = {
					@ApiResponse(responseCode = "200", description = "Businesses fetched successfully"),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "400", description = "Could not fetch businesses")
			}
	)
	@GetMapping("/get/all")
	public ResponseEntity<?> getBusinesses() {
		try {
			List<Business> businesses = businessService.findAll();
			return new ResponseEntity<>(businesses, HttpStatus.OK);
		}
		catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@Operation(
			summary = "Delete business",
			description = "Deletes a single business",
			responses = {
					@ApiResponse(responseCode = "200", description = "Business deleted successfully"),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "400", description = "Could not find business")
			}
	)
	@DeleteMapping("/delete/single")
	public ResponseEntity<?> deleteBusiness(@RequestBody Long id) {
		try {
			Business business = businessService.findById(id);
			if (business == null) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			businessService.delete(business);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}

	}
}
