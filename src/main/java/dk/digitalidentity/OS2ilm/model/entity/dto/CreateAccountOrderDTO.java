package dk.digitalidentity.OS2ilm.model.entity.dto;

import lombok.Builder;

@Builder
public record CreateAccountOrderDTO(
	String personUuid,
	String userType,
	String userId,
	String affiliationUuid
) {}
