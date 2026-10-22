package dk.digitalidentity.OS2ilm.util;

public class CprUtil {
	private static final int CPR_LENGTH = 10;
	private static final int DASH_POSITION = 6;

	private CprUtil() {
		throw new IllegalStateException();
	}

	/**
	 * Removes whitespace and dashes. The only thing a CPR number is guaranteed to be is 10 characters
	 * long - it may contain letters - so nothing else is stripped, and a value of the wrong length is
	 * returned as-is rather than rejected. Use when writing a CPR number to our own storage.
	 */
	public static String normalize(String cpr) {
		if (cpr == null) {
			return null;
		}

		String normalized = cpr.replaceAll("[\\s-]", "");
		return normalized.isEmpty() ? null : normalized;
	}

	public static boolean isValid(String cpr) {
		String normalized = normalize(cpr);
		return normalized != null && normalized.length() == CPR_LENGTH;
	}

	/**
	 * Formats a CPR number for an external system. Without a dash requested the value is passed through
	 * untouched, so data written before this formatting existed keeps reaching the external system in the
	 * shape it always did. A value that is not a valid CPR number is also passed through, since a dash
	 * cannot be placed in it safely.
	 */
	public static String format(String cpr, boolean withDash) {
		if (cpr == null || !withDash || !isValid(cpr)) {
			return cpr;
		}

		String normalized = normalize(cpr);
		return normalized.substring(0, DASH_POSITION) + "-" + normalized.substring(DASH_POSITION);
	}
}
