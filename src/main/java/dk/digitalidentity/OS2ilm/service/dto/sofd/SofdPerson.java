package dk.digitalidentity.OS2ilm.service.dto.sofd;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SofdPerson {
	private String cpr;
	private String master;
	private String firstname;
	private String surname;
	private String uuid;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss") private String lastChanged;
	private String chosenName;
	private Set<SofdUser> users;
	private Set<SofdAffiliation> affiliations;
}
