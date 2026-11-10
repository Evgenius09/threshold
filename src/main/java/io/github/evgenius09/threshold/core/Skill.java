package io.github.evgenius09.threshold.core;

import java.util.Optional;

public enum Skill {
	SHADOW_BOLT(Organ.SHADOW_GLAND, BodySlot.ARMS, 20, 60),
	SHADOW_STEP(Organ.SHADOW_GLAND, BodySlot.LEGS, 20, 120),
	SHADOW_VEIL(Organ.SHADOW_GLAND, BodySlot.BACK, 35, 400),
	STORM_DISCHARGE(Organ.THUNDER_FANG, BodySlot.ARMS, 25, 100),
	STORM_DASH(Organ.THUNDER_FANG, BodySlot.LEGS, 20, 240),
	STORM_HALO(Organ.THUNDER_FANG, BodySlot.BACK, 35, 300);

	private final Organ organ;
	private final BodySlot slot;
	private final double energyCost;
	private final int cooldownTicks;

	Skill(Organ organ, BodySlot slot, double energyCost, int cooldownTicks) {
		this.organ = organ;
		this.slot = slot;
		this.energyCost = energyCost;
		this.cooldownTicks = cooldownTicks;
	}

	public Organ organ() {
		return organ;
	}

	public BodySlot slot() {
		return slot;
	}

	public double energyCost() {
		return energyCost;
	}

	public int cooldownTicks() {
		return cooldownTicks;
	}

	// No row uses CORE, so the core slot never yields a skill.
	public static Optional<Skill> of(Organ organ, BodySlot slot) {
		for (Skill skill : values())
			if (skill.organ == organ && skill.slot == slot) return Optional.of(skill);
		return Optional.empty();
	}
}
