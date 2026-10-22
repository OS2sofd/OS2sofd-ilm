package dk.digitalidentity.OS2ilm.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CprUtilTest {

	@Nested
	class Normalize {

		@Test
		void stripsDash() {
			assertThat(CprUtil.normalize("010190-1234")).isEqualTo("0101901234");
		}

		@Test
		void stripsSurroundingAndInternalWhitespace() {
			assertThat(CprUtil.normalize("  010190 1234 ")).isEqualTo("0101901234");
			assertThat(CprUtil.normalize("010190\t-\t1234")).isEqualTo("0101901234");
		}

		@Test
		void keepsLetters() {
			assertThat(CprUtil.normalize("0101XY1234")).isEqualTo("0101XY1234");
			assertThat(CprUtil.normalize("010190-12AB")).isEqualTo("01019012AB");
		}

		@Test
		void keepsAnythingThatIsNotWhitespaceOrDash() {
			assertThat(CprUtil.normalize("010190+1234")).isEqualTo("010190+1234");
			assertThat(CprUtil.normalize("010190/1234")).isEqualTo("010190/1234");
		}

		@Test
		void keepsValuesOfTheWrongLength() {
			assertThat(CprUtil.normalize("123456789")).isEqualTo("123456789");
			assertThat(CprUtil.normalize("01019012345")).isEqualTo("01019012345");
		}

		@Test
		void keepsAlreadyNormalizedValue() {
			assertThat(CprUtil.normalize("0101901234")).isEqualTo("0101901234");
		}

		@Test
		void returnsNullWhenNothingIsLeft() {
			assertThat(CprUtil.normalize(null)).isNull();
			assertThat(CprUtil.normalize("")).isNull();
			assertThat(CprUtil.normalize("   ")).isNull();
			assertThat(CprUtil.normalize(" - ")).isNull();
		}
	}

	@Nested
	class IsValid {

		@Test
		void acceptsTenCharacters() {
			assertThat(CprUtil.isValid("0101901234")).isTrue();
			assertThat(CprUtil.isValid("010190-1234")).isTrue();
			assertThat(CprUtil.isValid(" 010190 1234 ")).isTrue();
		}

		@Test
		void acceptsLettersAndOtherCharacters() {
			assertThat(CprUtil.isValid("0101XY1234")).isTrue();
			assertThat(CprUtil.isValid("010190+234")).isTrue();
		}

		@Test
		void rejectsWrongLength() {
			assertThat(CprUtil.isValid("123456789")).isFalse();
			assertThat(CprUtil.isValid("01019012345")).isFalse();
		}

		@Test
		void rejectsEmpty() {
			assertThat(CprUtil.isValid(null)).isFalse();
			assertThat(CprUtil.isValid("")).isFalse();
			assertThat(CprUtil.isValid("  ")).isFalse();
		}
	}

	@Nested
	class Format {

		@Test
		void addsDashWhenRequested() {
			assertThat(CprUtil.format("0101901234", true)).isEqualTo("010190-1234");
		}

		@Test
		void keepsDashWhenAlreadyPresent() {
			assertThat(CprUtil.format("010190-1234", true)).isEqualTo("010190-1234");
		}

		@Test
		void addsDashToValuesContainingLetters() {
			assertThat(CprUtil.format("0101XY1234", true)).isEqualTo("0101XY-1234");
			assertThat(CprUtil.format("01019012AB", true)).isEqualTo("010190-12AB");
		}

		@Test
		void normalizesWhitespaceBeforeAddingDash() {
			assertThat(CprUtil.format(" 010190 1234 ", true)).isEqualTo("010190-1234");
		}

		@Test
		void passesInvalidLengthThroughUnchanged() {
			assertThat(CprUtil.format("123456789", true)).isEqualTo("123456789");
			assertThat(CprUtil.format("01019012345", true)).isEqualTo("01019012345");
			assertThat(CprUtil.format("  odd value  ", true)).isEqualTo("  odd value  ");
		}

		@Test
		void leavesValueUntouchedWhenDashIsNotRequested() {
			assertThat(CprUtil.format("010190-1234", false)).isEqualTo("010190-1234");
			assertThat(CprUtil.format("0101901234", false)).isEqualTo("0101901234");
			assertThat(CprUtil.format(" 010190 1234 ", false)).isEqualTo(" 010190 1234 ");
		}

		@Test
		void returnsNullForNull() {
			assertThat(CprUtil.format(null, true)).isNull();
			assertThat(CprUtil.format(null, false)).isNull();
		}
	}
}
