package io.github.evgenius09.threshold;

import static io.github.evgenius09.threshold.core.Skill.SHADOW_BOLT;
import static io.github.evgenius09.threshold.core.Skill.SHADOW_VEIL;
import static io.github.evgenius09.threshold.core.Skill.STORM_DISCHARGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.evgenius09.threshold.core.SkillState;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Numbers come from tech.md 5.4 and 5.7, so a failure here means the code drifted from the spec.
class SkillStateTest {
	private static SkillState ticks(SkillState s, int n) {
		for (int i = 0; i < n; i++) s = s.tick();
		return s;
	}

	@Test
	void veilCostsAndCoolsDown() {
		SkillState s = SkillState.FULL.afterUse(SHADOW_VEIL);
		assertEquals(65.0, s.energy(), 1e-9);
		assertEquals(400, s.cooldown(SHADOW_VEIL));
		assertFalse(s.canUse(SHADOW_VEIL));
	}

	@Test
	void lowEnergyBlocksUse() {
		SkillState s = new SkillState(10.0, Map.of());
		assertFalse(s.canUse(SHADOW_BOLT));
		assertSame(s, s.afterUse(SHADOW_BOLT));
	}

	@Test
	void regenFourPerSecond() {
		assertEquals(84.0, ticks(new SkillState(80.0, Map.of()), 20).energy(), 1e-9);
		assertEquals(100.0, ticks(SkillState.FULL, 20).energy(), 1e-9);
	}

	@Test
	void boltCooldownClears() {
		SkillState used = SkillState.FULL.afterUse(SHADOW_BOLT);
		assertEquals(1, ticks(used, 59).cooldown(SHADOW_BOLT));
		SkillState s = ticks(used, 60);
		assertEquals(0, s.cooldown(SHADOW_BOLT));
		assertFalse(s.cooldowns().containsKey(SHADOW_BOLT));
		assertTrue(s.canUse(SHADOW_BOLT));
	}

	@Test
	void regenFromZeroAffordsBoltAfterFiveSeconds() {
		SkillState s = ticks(new SkillState(0.0, Map.of()), 100);
		assertTrue(s.canUse(SHADOW_BOLT));
		assertTrue(s.afterUse(SHADOW_BOLT).energy() >= 0.0);
	}

	@Test
	void regenStopsAtMax() {
		assertEquals(100.0, new SkillState(99.9, Map.of()).tick().energy(), 1e-9);
	}

	@Test
	void afterUseLeavesFullUntouched() {
		SkillState.FULL.afterUse(SHADOW_VEIL);
		assertEquals(100.0, SkillState.FULL.energy(), 1e-9);
		assertTrue(SkillState.FULL.cooldowns().isEmpty());
	}

	@Test
	void cooldownsTickIndependently() {
		SkillState s = ticks(SkillState.FULL.afterUse(SHADOW_BOLT).afterUse(STORM_DISCHARGE), 60);
		assertFalse(s.cooldowns().containsKey(SHADOW_BOLT));
		assertEquals(40, s.cooldown(STORM_DISCHARGE));
	}
}
