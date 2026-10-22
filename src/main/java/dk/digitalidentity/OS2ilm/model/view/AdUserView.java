package dk.digitalidentity.OS2ilm.model.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import dk.digitalidentity.OS2ilm.model.entity.enums.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Entity(name = "view_active_directory_accounts")
public class AdUserView {

	@Id
	@Column
	@JsonProperty("id")
	private long id;

	@Column
	@JsonProperty("name")
	private String name;

	@Column
	@Enumerated(EnumType.STRING)
	@JsonProperty("status")
	private AccountStatus status;

	@Column
	@JsonProperty("startDate")
	private LocalDate startDate;

	@Column
	@JsonProperty("stopDate")
	private LocalDate stopDate;

	@Column
	@JsonProperty("adGroups")
	private String adGroups;
}
