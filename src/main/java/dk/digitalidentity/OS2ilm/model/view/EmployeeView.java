package dk.digitalidentity.OS2ilm.model.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import dk.digitalidentity.OS2ilm.model.entity.enums.ApproveStatus;
import dk.digitalidentity.OS2ilm.model.entity.enums.EmployeeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.Predicate;
import lombok.Getter;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Getter
@Entity(name = "view_employee")
public class EmployeeView {

	@Id
	@Column
	@JsonProperty("id")
	private long id;

	@Column
	@JsonProperty("name")
	private String name;

	@Column
	@Enumerated(EnumType.STRING)
	@JsonProperty("approved")
	private ApproveStatus approved;

	@Column
	@JsonProperty("cvr")
	private String cvr;

	@Column
	@JsonProperty("orgUnitName")
	private String orgUnitName;

	@Column
	@JsonProperty("startDate")
	private String startDate;

	@Column
	@Enumerated(EnumType.STRING)
	@JsonProperty("integrated")
	private EmployeeStatus integrated;

	@Column
	@JsonProperty("stopDate")
	private String stopDate;

	@Column
	@JsonProperty("businessId")
	private long businessId;

	@Column
	@JsonProperty("samAccountName")
	private String samAccountName;

	public static Specification<EmployeeView> findByCvr(List<String> cvrList) {
		return (root, query, criteriaBuilder) -> {

			// Build OR conditions for each CVR using LIKE
			Predicate[] predicates = cvrList.stream()
					.map(cvr -> criteriaBuilder.like(root.get("cvr"), "%" + cvr + "%"))
					.toArray(Predicate[]::new);

			return criteriaBuilder.or(predicates);
		};
	}
}
