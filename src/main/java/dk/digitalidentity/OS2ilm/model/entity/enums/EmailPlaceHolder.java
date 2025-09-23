package dk.digitalidentity.OS2ilm.model.entity.enums;

import lombok.Getter;

@Getter
public enum EmailPlaceHolder {
    PLACEHOLDER_NAME( "Pladsholdere til navn", "PLACEHOLDER_NAME" ),
    PLACEHOLDER_CODE( "Pladsholdere til engangskode", "PLACEHOLDER_CODE" ),
    PLACEHOLDER_PASSWORD( "Pladsholdere til kodeord", "PLACEHOLDER_PASSWORD" );

    private String displayName;
    private String key;

    private EmailPlaceHolder(String displayName, String key) {
        this.key = key;
        this.displayName = displayName;
    }
}
