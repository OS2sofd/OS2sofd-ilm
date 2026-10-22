package dk.digitalidentity.OS2ilm.util;

import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.dto.NamesDTO;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class NameUtils {

	private static final List<String> LEGAL_FORM_SUFFIXES = List.of("A/S", "ApS", "I/S", "K/S", "P/S", "IVS");

	public static String getPersonInitials(Employee employee) {
		NamesDTO names = splitName(employee);
		StringBuilder initials = new StringBuilder();
		// We want the initials of all the first names
		if (!names.firstName().isBlank()) {
			for (String firstName : names.firstName().split("\\s+")) {
				if (!firstName.isBlank()) {
					initials.append(firstName.charAt(0));
				}
			}
		}
		if (!names.lastName().isBlank()) {
			initials.append(names.lastName().charAt(0));
		}
		return initials.toString().toUpperCase();
	}

	public static String getCompanyInitials(String name) {
		StringBuilder initials = new StringBuilder();
		String[] words = name.trim().split("\\s+");
		for (String word : words) {
			if (!word.isBlank() && !LEGAL_FORM_SUFFIXES.contains(word)) {
				initials.append(word.charAt(0));
			}
		}
		return initials.toString().toUpperCase();
	}

	public static @NotNull NamesDTO splitName(Employee employee) {
		String name = employee.getName();
		if (name == null || name.isBlank()) {
			return new NamesDTO("", "");
		}

		String[] parts = name.trim().split("\\s+");
		String surname = parts[parts.length - 1];
		String firstname = parts.length > 1 ? String.join(" ", Arrays.copyOfRange(parts, 0, parts.length - 1)) : "";

		return new NamesDTO(firstname, surname);
	}
}
