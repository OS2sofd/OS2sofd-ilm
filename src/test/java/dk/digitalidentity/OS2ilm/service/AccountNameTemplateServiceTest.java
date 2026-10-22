package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.exception.InvalidAccountNameException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountNameTemplateServiceTest {

    @Mock
    private AdUserService adUserService;

    private AccountNameTemplateService accountNameTemplateService;

    private static Employee employeeOf(String name, String companyName) {
        Business business = new Business();
        business.setName(companyName);

        Employee employee = new Employee();
        employee.setName(name);
        employee.setBusiness(business);
        return employee;
    }

    private static AdUser adUserNamed(String name) {
        AdUser user = new AdUser();
        user.setName(name);
        return user;
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        accountNameTemplateService = new AccountNameTemplateService(adUserService);
    }

    @Nested
    class Resolve {

        @Test
        void shouldSubstituteAllStaticPlaceholders() {
            // Arrange
            Employee employee = employeeOf("Jens Jensen", "Digital Identity");
            String template = "{virksomhed-navn}-{virksomhed-initialer}-{person-fornavn}-{person-efternavn}-{person-navn}-{person-initialer}";

            // Act
            String result = accountNameTemplateService.resolve(template, employee);

            // Assert
            assertThat(result).isEqualTo("Digital Identity-DI-Jens-Jensen-Jens Jensen-JJ");
        }

        @Test
        void shouldResolveSequenceNumberBasedOnExistingAdUsersWithMatchingPrefix() {
            // Arrange
            Employee employee = employeeOf("Jens Jensen", "Digital Identity");
            String template = "lev-{person-initialer}-{løbenummer}";
            when(adUserService.findByNameStartingWith("lev-JJ-")).thenReturn(List.of(
                    adUserNamed("lev-JJ-0001"),
                    adUserNamed("lev-JJ-0003"),
                    adUserNamed("lev-JJ-not-a-number")
            ));

            // Act
            String result = accountNameTemplateService.resolve(template, employee);

            // Assert — next number after the highest existing numeric suffix (3), zero-padded to 4 digits
            assertThat(result).isEqualTo("lev-JJ-0004");
        }

        @Test
        void shouldStartSequenceAtOneWhenNoExistingUsersMatchPrefix() {
            // Arrange
            Employee employee = employeeOf("Jens Jensen", "Digital Identity");
            String template = "lev-{person-initialer}-{løbenummer}";
            when(adUserService.findByNameStartingWith("lev-JJ-")).thenReturn(List.of());

            // Act
            String result = accountNameTemplateService.resolve(template, employee);

            // Assert
            assertThat(result).isEqualTo("lev-JJ-0001");
        }

        @Test
        void shouldResolveWithoutConsultingAdUserServiceWhenTemplateHasNoSequenceToken() {
            // Arrange
            Employee employee = employeeOf("Jens Jensen", "Digital Identity");
            String template = "lev-{person-initialer}";

            // Act
            String result = accountNameTemplateService.resolve(template, employee);

            // Assert
            assertThat(result).isEqualTo("lev-JJ");
        }

        @Test
        void shouldTreatNullEmployeeNameAsEmptyString() {
            // Arrange
            Employee employee = employeeOf(null, "Digital Identity");
            String template = "{person-navn}";

            // Act
            String result = accountNameTemplateService.resolve(template, employee);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class ValidateTemplateSyntax {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void shouldRejectBlankTemplate(String template) {
            // Act + Assert
            assertThatThrownBy(() -> accountNameTemplateService.validateTemplateSyntax(template))
                    .isInstanceOf(InvalidAccountNameException.class);
        }

        @Test
        void shouldRejectUnknownPlaceholderToken() {
            // Act + Assert
            assertThatThrownBy(() -> accountNameTemplateService.validateTemplateSyntax("lev-{unknown-token}"))
                    .isInstanceOf(InvalidAccountNameException.class);
        }

        @Test
        void shouldRejectSequenceTokenWhenNotInFinalPosition() {
            // Act + Assert
            assertThatThrownBy(() -> accountNameTemplateService.validateTemplateSyntax("lev-{løbenummer}-{person-initialer}"))
                    .isInstanceOf(InvalidAccountNameException.class);
        }

        @Test
        void shouldAcceptSequenceTokenInFinalPosition() {
            // Act + Assert
            assertThatCode(() -> accountNameTemplateService.validateTemplateSyntax("lev-{person-initialer}-{løbenummer}"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldAcceptTemplateWithoutSequenceToken() {
            // Act + Assert
            assertThatCode(() -> accountNameTemplateService.validateTemplateSyntax("lev-{virksomhed-initialer}-{person-initialer}"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldAcceptTemplateWithNoPlaceholdersAtAll() {
            // Act + Assert
            assertThatCode(() -> accountNameTemplateService.validateTemplateSyntax("static-name"))
                    .doesNotThrowAnyException();
        }
    }
}
