package dk.digitalidentity.OS2ilm.config;

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Component;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.reporter.suppliers.ReporterSupplier;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IlmReportSupplier implements ReporterSupplier {
	private final GitProperties gitProperties;

	private final IlmConfiguration ilmConfiguration;

	@Override
	public Supplier<String> getCustomerNameSupplier() {
		return null;
	}

	@Override
	public Supplier<String> getProductNameSupplier() {
		return (() -> "OS2ilm");
	}

	@Override
	public Supplier<String> getVersionSupplier() {
			return (() -> String.format("%s - %s", ilmConfiguration.getVersion(), getShortCommitId()));
	}

	@Override
	public Supplier<Map<String, String>> getAttributeMapSupplier() {
		return (() -> Map.of("git_commit_id", getShortCommitId(),
							 "git_branch", getBranch()));
	}

	private String getBranch() {
		return gitProperties.getBranch() != null ? gitProperties.getBranch() : "N/A";
	}

	private String getShortCommitId() {
		return gitProperties.getShortCommitId() != null ? gitProperties.getShortCommitId() : "N/A";
	}
}
