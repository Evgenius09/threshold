package io.github.evgenius09.threshold;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

// core/ must stay testable without the game on the classpath (tech.md, section 5).
class CorePurityTest {
	private static final Path CORE_DIR = Path.of("src/main/java/io/github/evgenius09/threshold/core");
	private static final Pattern GAME_IMPORT =
			Pattern.compile("^\\s*import\\s+(static\\s+)?(net\\.minecraft|com\\.mojang|net\\.fabricmc)\\.");

	@Test
	void coreDirExists() {
		// Without this a moved package would make the purity check pass on nothing.
		assertTrue(Files.isDirectory(CORE_DIR), "Missing " + CORE_DIR.toAbsolutePath());
	}

	@Test
	void coreHasNoGameImports() throws IOException {
		List<String> violations = new ArrayList<>();
		try (Stream<Path> files = Files.walk(CORE_DIR)) {
			for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
				List<String> lines = Files.readAllLines(file);
				for (int i = 0; i < lines.size(); i++) {
					if (GAME_IMPORT.matcher(lines.get(i)).find()) {
						violations.add(file + ":" + (i + 1));
					}
				}
			}
		}
		assertTrue(violations.isEmpty(), "Game imports in core/: " + violations);
	}
}
