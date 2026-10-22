package dk.digitalidentity.OS2ilm.model.entity;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Audited
@Getter
@Setter
@Entity(name = "activation_time_frame")
public class ActivationTimeFrame {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String name;

	@Column
	private LocalTime startTime;

	@Column
	private LocalTime stopTime;

	@Column
	private boolean excludeWeekends;

	@OneToMany(mappedBy = "activationTimeFrame", cascade = CascadeType.ALL)
	@NotAudited
	@JsonIgnore
	private List<AdUser> adUsers = new ArrayList<>();

	@OneToMany(mappedBy = "activationTimeFrame")
	@NotAudited
	private List<Employee> employees;

	@OneToMany(mappedBy = "activationTimeFrame")
	@NotAudited
	private List<Business> businesses;

	public String getDescription() {
		return "Fra " + startTime + " til " + stopTime + " og weekend: " + (excludeWeekends ? "ikke inkluderet" : "inkluderet");
	}
}
