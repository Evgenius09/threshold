package io.github.evgenius09.threshold;

import static io.github.evgenius09.threshold.core.BodySlot.ARMS;
import static io.github.evgenius09.threshold.core.BodySlot.BACK;
import static io.github.evgenius09.threshold.core.BodySlot.CORE;
import static io.github.evgenius09.threshold.core.BodySlot.LEGS;
import static io.github.evgenius09.threshold.core.Organ.SHADOW_GLAND;
import static io.github.evgenius09.threshold.core.Organ.THUNDER_FANG;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.evgenius09.threshold.core.BodySlot;
import io.github.evgenius09.threshold.core.Element;
import io.github.evgenius09.threshold.core.Organ;
import io.github.evgenius09.threshold.core.Skill;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// Numbers come from tech.md 5.2–5.4, so a failure here means the code drifted from the spec.
class SkillTest {
	@Test
	void organElements() {
		assertEquals(Element.SHADOW, SHADOW_GLAND.element());
		assertEquals(Element.STORM, THUNDER_FANG.element());
	}

	@Test
	void ofMatchesTable() {
		Object[][] table = {
			{SHADOW_GLAND, ARMS, Skill.SHADOW_BOLT, 20.0, 60},
			{SHADOW_GLAND, LEGS, Skill.SHADOW_STEP, 20.0, 120},
			{SHADOW_GLAND, BACK, Skill.SHADOW_VEIL, 35.0, 400},
			{THUNDER_FANG, ARMS, Skill.STORM_DISCHARGE, 25.0, 100},
			{THUNDER_FANG, LEGS, Skill.STORM_DASH, 20.0, 240},
			{THUNDER_FANG, BACK, Skill.STORM_HALO, 35.0, 300},
		};
		for (Object[] row : table) {
			Skill skill = (Skill) row[2];
			assertEquals(Optional.of(skill), Skill.of((Organ) row[0], (BodySlot) row[1]));
			assertEquals((double) row[3], skill.energyCost(), 1e-9, skill + " cost");
			assertEquals((int) row[4], skill.cooldownTicks(), skill + " cooldown");
		}
	}

	@Test
	void ofCoreIsEmptyForEveryOrgan() {
		for (Organ organ : Organ.values()) assertTrue(Skill.of(organ, CORE).isEmpty(), organ.toString());
	}

	@Test
	void slotOrder() {
		// The order is the menu and storage order (tech.md 5.2).
		assertArrayEquals(new BodySlot[] {ARMS, LEGS, BACK, CORE}, BodySlot.values());
	}
}
