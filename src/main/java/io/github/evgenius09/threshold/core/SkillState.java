package io.github.evgenius09.threshold.core;

import java.util.EnumMap;
import java.util.Map;

public record SkillState(double energy, Map<Skill, Integer> cooldowns) {
	public static final double MAX_ENERGY = 100.0;
	public static final double REGEN_PER_TICK = 0.2;
	public static final SkillState FULL = new SkillState(MAX_ENERGY, Map.of());

	public SkillState {
		cooldowns = Map.copyOf(cooldowns);
	}

	public int cooldown(Skill skill) {
		return cooldowns.getOrDefault(skill, 0);
	}

	// Summing 0.2 per tick drifts below whole numbers, so the tolerance keeps a skill ready on time.
	public boolean canUse(Skill skill) {
		return energy + 1e-9 >= skill.energyCost() && cooldown(skill) == 0;
	}

	public SkillState afterUse(Skill skill) {
		if (!canUse(skill)) return this;
		Map<Skill, Integer> next = new EnumMap<>(Skill.class);
		next.putAll(cooldowns);
		next.put(skill, skill.cooldownTicks());
		return new SkillState(Math.max(0.0, energy - skill.energyCost()), next);
	}

	public SkillState tick() {
		Map<Skill, Integer> next = new EnumMap<>(Skill.class);
		cooldowns.forEach((skill, left) -> {
			if (left > 1) next.put(skill, left - 1);
		});
		return new SkillState(Math.min(MAX_ENERGY, energy + REGEN_PER_TICK), next);
	}
}
