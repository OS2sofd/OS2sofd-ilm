package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.controller.api.InternalApiController;
import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.OrgUnit;
import dk.digitalidentity.OS2ilm.model.exception.NotCommittedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InternalAPIService {
	private final BusinessService businessService;
	private final AdUserService adUserService;
	private final OrgUnitService orgUnitService;
	private final EmployeeService employeeService;
	private final SofdService sofdService;


	@Transactional(rollbackFor = NotCommittedException.class)
	public String updateEmployee(Employee employee, InternalApiController.EditEmployeeRecord editEmployeeRecord) {
		String changelog = "\n";

		if (!Objects.equals(editEmployeeRecord.name(), employee.getName())) {
			changelog += "Name changed from " + employee.getName() + " to " + editEmployeeRecord.name() + "\n";
			employee.setName(editEmployeeRecord.name());
		}
		if (!Objects.equals(editEmployeeRecord.email(), employee.getEmail())) {
			changelog += "Email changed from " + employee.getEmail() + " to " + editEmployeeRecord.email() + "\n";
			employee.setEmail(editEmployeeRecord.email());
		}
		if (!Objects.equals(editEmployeeRecord.phoneNumber(), employee.getPhone())) {
			changelog += "PhoneNumber changed from " + employee.getPhone() + " to " + editEmployeeRecord.phoneNumber() + "\n";
			employee.setPhone(editEmployeeRecord.phoneNumber());
		}
		if (!Objects.equals(editEmployeeRecord.mitIdUuid(), employee.getMitIdUuid())) {
			changelog += "MitIdUuid changed from " + employee.getMitIdUuid() + " to " + editEmployeeRecord.mitIdUuid() + "\n";
			employee.setMitIdUuid(editEmployeeRecord.mitIdUuid());
		}
		if (!Objects.equals(editEmployeeRecord.cpr(), employee.getCpr())) {
			changelog += "Cpr changed from " + employee.getCpr() + " to " + editEmployeeRecord.cpr() + "\n";
			employee.setCpr(editEmployeeRecord.cpr());
		}
		if ((editEmployeeRecord.businessId() != null && employee.getBusiness() == null) || !Objects.equals(editEmployeeRecord.businessId(), employee.getBusiness().getId())) {
			Business business = businessService.findById(editEmployeeRecord.businessId());

			if (business == null && !editEmployeeRecord.dryRun()) {
				throw new IllegalArgumentException("Business not found!");
			}
			changelog += "BusinessId changed from " + employee.getBusiness().getId() + " to " + editEmployeeRecord.businessId() + "\n";
			employee.setBusiness(business);
		}
		if (!Objects.equals(editEmployeeRecord.approveStatus(), employee.getApproved())) {
			changelog += "Approved changed from " + employee.getApproved() + " to " + editEmployeeRecord.approveStatus() + "\n";
			employee.setApproved(editEmployeeRecord.approveStatus());
		}
		if (!Objects.equals(editEmployeeRecord.cvr(), employee.getCvr())) {
			changelog += "Cvr changed from " + employee.getCvr() + " to " + editEmployeeRecord.cvr() + "\n";
			employee.setCvr(editEmployeeRecord.cvr());
		}
		if (!Objects.equals(editEmployeeRecord.positionName(), employee.getPositionName())) {
			changelog += "PositionName changed from " + employee.getPositionName() + " to " + editEmployeeRecord.positionName() + "\n";
			employee.setPositionName(editEmployeeRecord.positionName());
		}
		if (!Objects.equals(editEmployeeRecord.startDate(), employee.getStartDate())) {
			changelog += "StartDate changed from " + employee.getStartDate() + " to " + editEmployeeRecord.startDate() + "\n";
			employee.setStartDate(editEmployeeRecord.startDate());
		}
		if (!Objects.equals(editEmployeeRecord.stopDate(), employee.getStopDate())) {
			changelog += "StopDate changed from " + employee.getStopDate() + " to " + editEmployeeRecord.stopDate() + "\n";
			employee.setStopDate(editEmployeeRecord.stopDate());
		}
		if ((editEmployeeRecord.adUserId() != null && employee.getUser() == null) || editEmployeeRecord.adUserId() != null && employee.getUser() != null && !Objects.equals(editEmployeeRecord.adUserId(), employee.getUser().getId())) {
			AdUser user = adUserService.findById(editEmployeeRecord.adUserId());

			if (user == null && !editEmployeeRecord.dryRun()) {
				throw new IllegalArgumentException("AdUser not found!");
			}
			changelog += "AdUserId changed from " + employee.getUser().getId() + " to " + editEmployeeRecord.adUserId() + "\n";
			employee.setUser(user);
		}
		if (!Objects.equals(editEmployeeRecord.blocked(), employee.isBlocked())) {
			changelog += "Blocked changed from " + employee.isBlocked() + " to " + editEmployeeRecord.blocked() + "\n";
			employee.setBlocked(editEmployeeRecord.blocked());
		}
		if ((editEmployeeRecord.orgUnitUuid() != null && employee.getOrgUnit() == null) || !Objects.equals(editEmployeeRecord.orgUnitUuid(), employee.getOrgUnit().getUuid())) {
			OrgUnit ou = orgUnitService.findOrgUnitByUuid(editEmployeeRecord.orgUnitUuid());
			if (ou == null && !editEmployeeRecord.dryRun()) {
				throw new IllegalArgumentException("OrgUnit not found!");
			}
			changelog += "OrgUnitUuid changed from " + employee.getOrgUnit().getUuid() + " to " + editEmployeeRecord.orgUnitUuid() + "\n";
			employee.setOrgUnit(ou);
		}

		if (editEmployeeRecord.dryRun()) {
			throw new NotCommittedException(changelog);
		}

		employeeService.save(employee);
		if (StringUtils.hasText(employee.getCpr())) {
			sofdService.sofdCreateAndUpdateHelper(employee);
		}

		return "The following changes have been made in the DB: \n" + changelog;
	}
}
