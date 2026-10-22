package dk.digitalidentity.OS2ilm.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ChangelogService {
	private final String html;

	public ChangelogService() {
		try (var in = new ClassPathResource("CHANGELOG.md").getInputStream()) {
			String markdown = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			html = HtmlRenderer.builder().escapeHtml(true).build().render(Parser.builder().build().parse(markdown));
		}
		catch (IOException e) {
			throw new UncheckedIOException("Could not read CHANGELOG.md from classpath", e);
		}
	}

	public String getHtml() {
		return html;
	}
}
