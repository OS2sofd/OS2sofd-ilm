package dk.digitalidentity.OS2ilm.model.entity.enums;

import lombok.Getter;

@Getter
public enum CmsPlaceHolder {
    PLACEHOLDER_BUSINESS_PREFIX("Virksomhedspræfiks");

    private String value;

    private CmsPlaceHolder(String value) {
        this.value = value;
    }
}
