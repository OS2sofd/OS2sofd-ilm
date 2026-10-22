package dk.digitalidentity.OS2ilm.util;

import java.util.List;

import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import jakarta.persistence.criteria.Predicate;

import dk.digitalidentity.OS2ilm.model.view.ApprovedEmployeeView;
import dk.digitalidentity.OS2ilm.model.view.EmployeeView;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.domain.Specification;

public class DatatableSpecBuilderUtil {

	private DatatableSpecBuilderUtil() {

	}

	@NotNull
	public static Specification<ApprovedEmployeeView> getApprovedEmployeeViewSpecification(List<String> cvrList) {
		return ApprovedEmployeeView.findByCvr(cvrList);
	}

	@NotNull
	public static Specification<EmployeeView> getEmployeeViewSpecification(List<String> cvrList) {
		return EmployeeView.findByCvr(cvrList);
	}

	public static Specification<ApprovedEmployeeView> getLeaderSpecification(List<String> cvrs) {
		return (root, query, criteriaBuilder) -> {
			// CVR filtering
			Predicate[] cvrPredicates = cvrs.stream()
					.map(cvr -> criteriaBuilder.like(root.get("cvr"), "%" + cvr + "%"))
					.toArray(Predicate[]::new);
			Predicate cvrPredicate = criteriaBuilder.or(cvrPredicates);

			// Leaders see records awaiting their approval, and records they have rejected
			Predicate statusPredicate = criteriaBuilder.or(
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.AWAITING_APPROVAL),
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.DENIED)
			);

			return criteriaBuilder.and(cvrPredicate, statusPredicate);
		};
	}

	public static Specification<ApprovedEmployeeView> getAdminSpecification(List<String> cvrs) {
		return (root, query, criteriaBuilder) -> {
			// CVR filtering
			Predicate[] cvrPredicates = cvrs.stream()
					.map(cvr -> criteriaBuilder.like(root.get("cvr"), "%" + cvr + "%"))
					.toArray(Predicate[]::new);
			Predicate cvrPredicate = criteriaBuilder.or(cvrPredicates);

			// Admins see records that need admin approval, and rejected records
			Predicate statusPredicate = criteriaBuilder.or(
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.AWAITING_APPROVAL),
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.LEADER_APPROVED),
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.AWAITING_EXTENSION_APPROVAL),
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.DENIED)
			);

			return criteriaBuilder.and(cvrPredicate, statusPredicate);
		};
	}

	public static Specification<ApprovedEmployeeView> getLeaderSpecificationWithoutCVR() {
		return (root, query, criteriaBuilder) -> {
			// Leaders see records awaiting their approval, and records they have rejected (no CVR filtering)
			return criteriaBuilder.or(
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.AWAITING_APPROVAL),
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.DENIED)
			);
		};
	}

	public static Specification<ApprovedEmployeeView> getAdminSpecificationWithoutCVR() {
		return (root, query, criteriaBuilder) -> {
			// Admins see records that need admin approval, and rejected records (no CVR filtering)
			return criteriaBuilder.or(
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.AWAITING_APPROVAL),
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.LEADER_APPROVED),
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.AWAITING_EXTENSION_APPROVAL),
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.DENIED)
			);
		};
	}

}
