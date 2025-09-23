package dk.digitalidentity.OS2ilm.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity(name = "business")
public class Business {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

	@Column
	private String name;

	@Column(unique = true)
	private String cvr;

	@Column
	private String prefix;

	@OneToMany(mappedBy = "business")
	@JsonIgnore // needed for lazy loading
	private List<Employee> employees = new ArrayList<>();

}
