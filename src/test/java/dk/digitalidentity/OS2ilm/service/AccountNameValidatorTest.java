package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.exception.InvalidAccountNameException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountNameValidatorTest {

    private final AccountNameValidator validator = new AccountNameValidator();

    @Test
    void shouldAcceptValidAccountName() {
        // Act + Assert
        assertThatCode(() -> validator.validate("jsmith"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptAccountNameAtMaxLength() {
        // Arrange — exactly 20 characters
        String name = "abcdefghijklmnopqrst";

        // Act + Assert
        assertThatCode(() -> validator.validate(name))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectBlankAccountName(String name) {
        // Act + Assert
        assertThatThrownBy(() -> validator.validate(name))
                .isInstanceOf(InvalidAccountNameException.class);
    }

    @Test
    void shouldRejectAccountNameLongerThanMaxLength() {
        // Arrange — 21 characters
        String name = "abcdefghijklmnopqrstu";

        // Act + Assert
        assertThatThrownBy(() -> validator.validate(name))
                .isInstanceOf(InvalidAccountNameException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {".jsmith", "jsmith."})
    void shouldRejectAccountNameWithLeadingOrTrailingDot(String name) {
        // Act + Assert
        assertThatThrownBy(() -> validator.validate(name))
                .isInstanceOf(InvalidAccountNameException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "js\"mith", "js/mith", "js\\mith", "js[mith", "js]mith",
            "js:mith", "js;mith", "js|mith", "js=mith", "js,mith",
            "js+mith", "js*mith", "js?mith", "js<mith", "js>mith", "js@mith"
    })
    void shouldRejectAccountNameContainingDisallowedCharacters(String name) {
        // Act + Assert
        assertThatThrownBy(() -> validator.validate(name))
                .isInstanceOf(InvalidAccountNameException.class);
    }

    @Test
    void shouldAcceptAccountNameWithHyphenAndUnderscore() {
        // Act + Assert
        assertThatCode(() -> validator.validate("lev-jens_jensen"))
                .doesNotThrowAnyException();
    }
}
