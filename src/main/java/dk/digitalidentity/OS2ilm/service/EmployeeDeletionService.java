package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.dao.EmployeeDao;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeDeletionService {
	private final EmployeeDao employeeDao;

	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
	public void deleteAll(List<Employee> employees) {
		employeeDao.deleteAll(employees);
	}

	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
	public void deleteAllInAud(List<Employee> employees) {
		ArrayList<Long> ids = employees.stream()
				.map(Employee::getId)
				.collect(Collectors.toCollection(ArrayList::new));
		employeeDao.deleteInAud(ids);
	}
}
