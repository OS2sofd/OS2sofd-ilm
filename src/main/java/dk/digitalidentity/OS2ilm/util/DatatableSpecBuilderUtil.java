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

			// Leaders only see records awaiting their approval
			Predicate statusPredicate = criteriaBuilder.equal(root.get("approval"), ApproveStatus.AWAITING_APPROVAL);

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

			// Admins see records that need admin approval
			Predicate statusPredicate = criteriaBuilder.or(
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.AWAITING_APPROVAL),
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.LEADER_APPROVED),
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.AWAITING_EXTENSION_APPROVAL)
			);

			return criteriaBuilder.and(cvrPredicate, statusPredicate);
		};
	}

	public static Specification<ApprovedEmployeeView> getLeaderSpecificationWithoutCVR() {
		return (root, query, criteriaBuilder) -> {
			// Leaders only see records awaiting their approval (no CVR filtering)
			return criteriaBuilder.equal(root.get("approval"), ApproveStatus.AWAITING_APPROVAL);
		};
	}

	public static Specification<ApprovedEmployeeView> getAdminSpecificationWithoutCVR() {
		return (root, query, criteriaBuilder) -> {
			// Admins see records that need admin approval (no CVR filtering)
			return criteriaBuilder.or(
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.AWAITING_APPROVAL),
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.LEADER_APPROVED),
					criteriaBuilder.equal(root.get("approval"), ApproveStatus.AWAITING_EXTENSION_APPROVAL)
			);
		};
	}

}
