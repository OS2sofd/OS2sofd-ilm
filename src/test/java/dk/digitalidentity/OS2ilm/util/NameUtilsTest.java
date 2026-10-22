package dk.digitalidentity.OS2ilm.util;

import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.dto.NamesDTO;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class NameUtilsTest {

    private static Employee employeeNamed(String name) {
        Employee employee = new Employee();
        employee.setName(name);
        return employee;
    }

    @Nested
    class SplitName {

        @Test
        void shouldSplitFirstnameAndSurnameWhenNameHasTwoParts() {
            // Arrange
            Employee employee = employeeNamed("Jens Jensen");

            // Act
            NamesDTO result = NameUtils.splitName(employee);

            // Assert
            assertThat(result.firstName()).isEqualTo("Jens");
            assertThat(result.lastName()).isEqualTo("Jensen");
        }

        @Test
        void shouldJoinAllButLastTokenAsFirstnameWhenNameHasMultipleParts() {
            // Arrange
            Employee employee = employeeNamed("Anna Maria Poulsen");

            // Act
            NamesDTO result = NameUtils.splitName(employee);

            // Assert
            assertThat(result.firstName()).isEqualTo("Anna Maria");
            assertThat(result.lastName()).isEqualTo("Poulsen");
        }

        @Test
        void shouldLeaveFirstnameBlankWhenNameHasOnlyOneToken() {
            // Arrange
            Employee employee = employeeNamed("Madonna");

            // Act
            NamesDTO result = NameUtils.splitName(employee);

            // Assert
            assertThat(result.firstName()).isEmpty();
            assertThat(result.lastName()).isEqualTo("Madonna");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void shouldReturnBlankPartsWhenNameIsNullOrBlank(String name) {
            // Arrange
            Employee employee = employeeNamed(name);

            // Act
            NamesDTO result = NameUtils.splitName(employee);

            // Assert
            assertThat(result.firstName()).isEmpty();
            assertThat(result.lastName()).isEmpty();
        }

        @Test
        void shouldCollapseExtraWhitespaceBetweenNameParts() {
            // Arrange
            Employee employee = employeeNamed("  Jens   Jensen  ");

            // Act
            NamesDTO result = NameUtils.splitName(employee);

            // Assert
            assertThat(result.firstName()).isEqualTo("Jens");
            assertThat(result.lastName()).isEqualTo("Jensen");
        }
    }

    @Nested
    class GetPersonInitials {

        @Test
        void shouldReturnUppercaseFirstAndLastInitialsForTwoPartName() {
            // Arrange
            Employee employee = employeeNamed("jens jensen");

            // Act
            String result = NameUtils.getPersonInitials(employee);

            // Assert
            assertThat(result).isEqualTo("JJ");
        }

        @Test
        void shouldIncludeInitialsOfAllFirstNamesAndLastToken() {
            // Arrange
            Employee employee = employeeNamed("Anna Maria Poulsen");

            // Act
            String result = NameUtils.getPersonInitials(employee);

            // Assert — every first name contributes its initial, plus the surname
            assertThat(result).isEqualTo("AMP");
        }

        @Test
        void shouldReturnSingleInitialWhenNameHasOnlyOneToken() {
            // Arrange
            Employee employee = employeeNamed("Madonna");

            // Act
            String result = NameUtils.getPersonInitials(employee);

            // Assert
            assertThat(result).isEqualTo("M");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void shouldReturnEmptyStringWhenNameIsNullOrEmpty(String name) {
            // Arrange
            Employee employee = employeeNamed(name);

            // Act
            String result = NameUtils.getPersonInitials(employee);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetCompanyInitials {

        @ParameterizedTest
        @CsvSource({
                "Digital Identity, DI",
                "Digital Identity A/S, DI",
                "Digital Identity ApS, DI",
                "Odense Kommune I/S, OK",
                "Nordic Solutions K/S, NS",
                "Test Company P/S, TC",
                "Test Company IVS, TC"
        })
        void shouldStripLegalFormSuffixBeforeComputingInitials(String companyName, String expectedInitials) {
            // Act
            String result = NameUtils.getCompanyInitials(companyName);

            // Assert
            assertThat(result).isEqualTo(expectedInitials);
        }

        @Test
        void shouldReturnSingleInitialForOneWordCompanyName() {
            // Act
            String result = NameUtils.getCompanyInitials("Digitalidentity");

            // Assert
            assertThat(result).isEqualTo("D");
        }

        @Test
        void shouldCollapseExtraWhitespaceBetweenWords() {
            // Act
            String result = NameUtils.getCompanyInitials("  Digital   Identity  ");

            // Assert
            assertThat(result).isEqualTo("DI");
        }
    }
}
