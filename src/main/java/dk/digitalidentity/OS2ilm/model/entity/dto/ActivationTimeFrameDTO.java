package dk.digitalidentity.OS2ilm.model.entity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActivationTimeFrameDTO {
	private long id;
	private String formattedStartTime;
	private String formattedStopTime;
	private boolean excludeWeekends;
}
