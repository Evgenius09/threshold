package io.github.evgenius09.threshold;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

// Every registered item needs a Russian name, or the player sees a raw key (tech.md, section 8).
class LangTest {
	private static final Path RU_RU = Path.of("src/main/resources/assets/threshold_gates/lang/ru_ru.json");
	// Kept explicit so the test runs without loading Minecraft registries.
	private static final List<String> ITEM_IDS = List.of("debug_shard");

	@Test
	void everyItemHasRussianName() throws IOException {
		JsonObject lang = JsonParser.parseString(Files.readString(RU_RU)).getAsJsonObject();
		for (String id : ITEM_IDS) {
			String key = "item.threshold_gates." + id;
			assertTrue(lang.has(key), "Missing key in ru_ru.json: " + key);
			assertTrue(!lang.get(key).getAsString().isBlank(), "Blank value in ru_ru.json: " + key);
		}
	}
}
