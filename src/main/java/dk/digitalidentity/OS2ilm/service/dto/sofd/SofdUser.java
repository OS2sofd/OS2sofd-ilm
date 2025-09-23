package dk.digitalidentity.OS2ilm.service.dto.sofd;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.ALWAYS)
public class SofdUser {
	private String master;
	private String masterId;
	private String uuid;
	private String userId;
	private String userType;
}
