package dk.digitalidentity.OS2ilm.controller.api;

import java.util.List;

import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import dk.digitalidentity.OS2ilm.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RequestMapping("/api/employee")
@Tag(name = "Employee API", description = "Endpoints for managing employees")
@RequiredArgsConstructor
@RestController
public class EmployeeApiController {
	private final EmployeeService employeeService;

	@Operation(
			summary = "Output employees",
			description = "Outputs a list of all employees",
			responses = {
					@ApiResponse(responseCode = "200", description = "Employees fetched successfully"),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "400", description = "Could not fetch employees")
			}
	)
	@GetMapping("/get/all")
	public ResponseEntity<?> getEmployees() {
		try {
			List<Employee> employees = employeeService.findAll();
			return new ResponseEntity<>(employees, HttpStatus.OK);
		}
		catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@Operation(
			summary = "Delete employee",
			description = "Deletes a single employee",
			responses = {
					@ApiResponse(responseCode = "200", description = "Employee deleted successfully"),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "400", description = "Could not find employee")
			}
	)
	@DeleteMapping("/delete/single")
	public ResponseEntity<?> deleteEmployee(@RequestBody Long id) {
		try {
			Employee employee = employeeService.findById(id);
			if (employee == null) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			employeeService.delete(employee);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@Operation(
			summary = "Count awaiting employees",
			description = "Counts the number of employees that are awaiting approval",
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully counted pending approvals"),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "400", description = "Could not count pending approvals")
			}
	)
	@GetMapping("/pending-approval/count")
	public ResponseEntity<?> countPendingApprovals() {
		try {
			List<ApproveStatus> statuses = new ArrayList<>();
			statuses.add(ApproveStatus.LEADER_APPROVED);
			statuses.add(ApproveStatus.AWAITING_APPROVAL);

			List<Employee> pendingApprovals = employeeService.findEmployeesWithPendingApprovals(statuses);
			return new ResponseEntity<>(pendingApprovals.size(), HttpStatus.OK);
		}
		catch (Exception ex) {
			return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
}
