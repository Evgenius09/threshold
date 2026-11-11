package io.github.evgenius09.threshold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

// Every registered item needs a Russian name, or the player sees a raw key (tech.md, section 8).
class LangTest {
	private static final Path RU_RU = Path.of("src/main/resources/assets/threshold_gates/lang/ru_ru.json");
	// Kept explicit so the test runs without loading Minecraft registries.
	private static final Set<String> KEYS = Set.of(
			"item.threshold_gates.shadow_gland",
			"item.threshold_gates.shadow_gland.lore",
			"item.threshold_gates.thunder_fang",
			"item.threshold_gates.thunder_fang.lore",
			"item.threshold_gates.organ.element",
			"element.threshold_gates.steel",
			"element.threshold_gates.storm",
			"element.threshold_gates.shadow");

	private static JsonObject lang() throws IOException {
		return JsonParser.parseString(Files.readString(RU_RU)).getAsJsonObject();
	}

	@Test
	void ruRuHasExactlyTheExpectedKeys() throws IOException {
		// Exact match also catches stale keys, which tech.md 16 item 7 forbids.
		assertEquals(KEYS, lang().keySet());
	}

	@Test
	void noBlankValues() throws IOException {
		for (var entry : lang().entrySet())
			assertFalse(entry.getValue().getAsString().isBlank(), "Blank value in ru_ru.json: " + entry.getKey());
	}
}
