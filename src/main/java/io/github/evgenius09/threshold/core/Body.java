package io.github.evgenius09.threshold.core;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record Body(Map<BodySlot, Organ> organs) {
	public Body {
		organs = Map.copyOf(organs);
	}

	public Set<Element> elements() {
		Set<Element> result = EnumSet.of(Element.STEEL);
		for (Organ organ : organs.values()) result.add(organ.element());
		return Set.copyOf(result);
	}

	public Optional<Skill> skill(BodySlot slot) {
		Organ organ = organs.get(slot);
		return organ == null ? Optional.empty() : Skill.of(organ, slot);
	}

	// CORE grants no skill, so the organ there pays off as defense instead.
	public double resistance(Element attack) {
		Organ core = organs.get(BodySlot.CORE);
		return core != null && core.element() == attack ? 0.5 : 1.0;
	}

	public Element keepOrSteel(Element selected) {
		return elements().contains(selected) ? selected : Element.STEEL;
	}
}
