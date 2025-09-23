package dk.digitalidentity.OS2ilm.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RandomPasswordGeneratorUtil {
	private static final String UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";
	private static final String NUMBERS = "0123456789";
	private static final String SPECIAL_CHARACTERS = "-!\"#$%&()*,./:;?@[]^_{}~+<=>";

	private static final int MIN_CATEGORIES = 3;

	private RandomPasswordGeneratorUtil() {

	}

	// Should use constant memory - O(1), pass through the file once and still guarantee uniform random selection.
	private static String getRandomWord(SecureRandom random) {
		try (InputStream is = RandomPasswordGeneratorUtil.class.getClassLoader().getResourceAsStream("kodeord.dat");
				BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

			String selectedWord = null;
			int count = 0;

			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.trim().isEmpty()) {
					count++;
					// Reservoir sampling: replace with probability 1/count
					if (random.nextInt(count) == 0) {
						selectedWord = line.trim();
					}
				}
			}

			if (selectedWord == null) {
				throw new RuntimeException("No words found in file");
			}

			return selectedWord;
		} catch (IOException e) {
			throw new RuntimeException("Could not load word list", e);
		}
	}


	public static String generatePassword(int length) {
		StringBuilder passwordBuilder = new StringBuilder();
		Random random = new SecureRandom();

		Set<String> usedCategories = new HashSet<>();

		List<String> allCategories = Arrays.asList(
				UPPER_CASE, LOWER_CASE, NUMBERS, SPECIAL_CHARACTERS
		);

		List<String> selectedCategories = new ArrayList<>(allCategories);
		Collections.shuffle(selectedCategories, random);

		for (int i = 0; i < MIN_CATEGORIES; i++) {
			String category = selectedCategories.get(i);
			char randomChar = getRandomCharacter(category, random);
			passwordBuilder.append(randomChar);
			usedCategories.add(category);
		}

		while (passwordBuilder.length() < length) {
			String category = allCategories.get(random.nextInt(allCategories.size()));
			char randomChar = getRandomCharacter(category, random);
			passwordBuilder.append(randomChar);
		}

		char[] passwordArray = passwordBuilder.toString().toCharArray();
		for (int i = passwordArray.length - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			char temp = passwordArray[i];
			passwordArray[i] = passwordArray[j];
			passwordArray[j] = temp;
		}

		String finalPassword = new String(passwordArray);

		if (!meetsRequirements(finalPassword, length)) {
			return generatePassword(length);
		}

		return finalPassword;
	}

	enum Category {
		NUMBERS, SPECIAL_CHARACTERS, WORD
	}
	public static String generateUserFriendlyPassword(int length) {
		StringBuilder passwordBuilder = new StringBuilder();
		SecureRandom random = new SecureRandom();

		List<Category> allCategories = Arrays.asList(
				Category.NUMBERS, Category.SPECIAL_CHARACTERS, Category.WORD
		);
		List<Category> someCategories = Arrays.asList(
				Category.NUMBERS, Category.WORD
		);

		// Shuffle the collection of categories and make random
		List<Category> selectedCategories = new ArrayList<>(allCategories);
		Collections.shuffle(selectedCategories, random);
		while (passwordBuilder.length() < length) {
			int size = selectedCategories.size();
			for (int i = 0; i < size; i++) {
				if (passwordBuilder.length() >= length) {
					break;
				}
				Category category = selectedCategories.removeFirst();
				switch (category) {
					case NUMBERS -> {
						char randomChar = getRandomCharacter(NUMBERS, random);
						passwordBuilder.append(randomChar);
					}
					case SPECIAL_CHARACTERS -> {
						char randomChar = getRandomCharacter(SPECIAL_CHARACTERS, random);
						passwordBuilder.append(randomChar);
					}
					case WORD -> {
						String word = findAppropriateWord(passwordBuilder.length(), length, random);
						if (word != null) {
							passwordBuilder.append(word);
						}
					}
					default -> throw new IllegalStateException("Unexpected value: " + category);
				}
			}
			selectedCategories = new ArrayList<>(someCategories);
			Collections.shuffle(selectedCategories, random);
		}

		// If we've exactly matched the length, return the password
		if (!meetsRequirements(passwordBuilder.toString(), length)) {
			return generateUserFriendlyPassword(length);
		} else {
			return passwordBuilder.toString();
		}
	}

	private static String findAppropriateWord(int currentLength, int targetLength, SecureRandom random) {
		int maxWordLength = targetLength - currentLength;
		int maxAttempts = 50;

		for (int attempt = 0; attempt < maxAttempts; attempt++) {
			String word = getRandomWord(random);
			if (word.length() <= maxWordLength) {
				return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
			}
		}

		return null;
	}

	private static char getRandomCharacter(String characterSet, Random random) {
		return characterSet.charAt(random.nextInt(characterSet.length()));
	}

	public static boolean meetsRequirements(String password, int length) {
		if (password.length() < length) {
			return false;
		}

		int categoriesUsed = 0;

		if (password.chars().anyMatch(ch -> UPPER_CASE.indexOf(ch) >= 0)) categoriesUsed++;
		if (password.chars().anyMatch(ch -> LOWER_CASE.indexOf(ch) >= 0)) categoriesUsed++;
		if (password.chars().anyMatch(ch -> NUMBERS.indexOf(ch) >= 0)) categoriesUsed++;
		if (password.chars().anyMatch(ch -> SPECIAL_CHARACTERS.indexOf(ch) >= 0)) categoriesUsed++;

		return categoriesUsed >= MIN_CATEGORIES;
	}
}
