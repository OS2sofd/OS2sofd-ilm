package dk.digitalidentity.OS2ilm.model.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Entity(name = "view_approved_employees")
public class ApprovedEmployeeView {

	@Id
	@Column
	@JsonProperty("id")
	private long id;

	@Column
	@JsonProperty("name")
	private String name;

	@Column
	@JsonProperty("cpr")
	private String cpr;

	@Column
	@JsonProperty("cvr")
	private String cvr;

	@Column
	@Enumerated(EnumType.STRING)
	@JsonProperty("approval")
	private ApproveStatus approval;

	public static Specification<ApprovedEmployeeView> findByCvr(List<String> cvrList) {
		return (root, query, criteriaBuilder) -> {

			// Build OR conditions for each CVR using LIKE
			Predicate[] predicates = cvrList.stream()
					.map(cvr -> criteriaBuilder.like(root.get("cvr"), "%" + cvr + "%"))
					.toArray(Predicate[]::new);

			return criteriaBuilder.or(predicates);
		};
	}
}
