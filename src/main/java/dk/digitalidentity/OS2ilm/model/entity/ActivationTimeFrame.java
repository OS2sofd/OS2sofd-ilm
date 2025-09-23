package dk.digitalidentity.OS2ilm.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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
	@JsonIgnore
	private List<AdUser> adUsers = new ArrayList<>();

	@OneToOne(mappedBy = "activationTimeFrame")
	private Employee employee;
}
