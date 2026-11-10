package io.github.evgenius09.threshold;

import static io.github.evgenius09.threshold.core.BodySlot.ARMS;
import static io.github.evgenius09.threshold.core.BodySlot.CORE;
import static io.github.evgenius09.threshold.core.Element.SHADOW;
import static io.github.evgenius09.threshold.core.Element.STEEL;
import static io.github.evgenius09.threshold.core.Element.STORM;
import static io.github.evgenius09.threshold.core.Organ.SHADOW_GLAND;
import static io.github.evgenius09.threshold.core.Organ.THUNDER_FANG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.evgenius09.threshold.core.Body;
import io.github.evgenius09.threshold.core.BodySlot;
import io.github.evgenius09.threshold.core.Organ;
import io.github.evgenius09.threshold.core.Skill;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

// Numbers come from tech.md 5.5, so a failure here means the code drifted from the spec.
class BodyTest {
	private final Body empty = new Body(Map.of());
	private final Body glandArmsFangCore = new Body(Map.of(ARMS, SHADOW_GLAND, CORE, THUNDER_FANG));
	private final Body fangOnly = new Body(Map.of(ARMS, THUNDER_FANG));

	@Test
	void emptyBody() {
		assertEquals(Set.of(STEEL), empty.elements());
		assertTrue(empty.skill(ARMS).isEmpty());
		assertEquals(1.0, empty.resistance(SHADOW), 1e-9);
	}

	@Test
	void glandInArmsFangInCore() {
		assertEquals(Set.of(STEEL, SHADOW, STORM), glandArmsFangCore.elements());
		assertEquals(Optional.of(Skill.SHADOW_BOLT), glandArmsFangCore.skill(ARMS));
		assertTrue(glandArmsFangCore.skill(CORE).isEmpty());
		assertEquals(0.5, glandArmsFangCore.resistance(STORM), 1e-9);
		assertEquals(1.0, glandArmsFangCore.resistance(SHADOW), 1e-9);
		assertEquals(1.0, glandArmsFangCore.resistance(STEEL), 1e-9);
	}

	@Test
	void keepOrSteel() {
		assertEquals(STEEL, fangOnly.keepOrSteel(SHADOW));
		assertEquals(SHADOW, glandArmsFangCore.keepOrSteel(SHADOW));
	}

	@Test
	void bodyIgnoresLaterChangesToSourceMap() {
		Map<BodySlot, Organ> source = new HashMap<>();
		source.put(ARMS, SHADOW_GLAND);
		Body body = new Body(source);
		source.put(CORE, THUNDER_FANG);
		assertFalse(body.organs().containsKey(CORE));
		assertEquals(Set.of(STEEL, SHADOW), body.elements());
	}
}
