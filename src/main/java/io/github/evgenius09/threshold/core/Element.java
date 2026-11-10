package io.github.evgenius09.threshold.core;

import java.util.Set;

public enum Element {
	STEEL(0xC0C8D0),
	STORM(0xFFE04A),
	SHADOW(0x8A3FD1);

	private final int color;

	Element(int color) {
		this.color = color;
	}

	public int color() {
		return color;
	}

	// STEEL is the neutral baseline every player has, so it never shifts damage either way.
	public static double multiplier(Element attack, Element defender) {
		if (attack == STEEL || defender == STEEL) return 1.0;
		return attack == defender ? 0.5 : 1.5;
	}

	// Walks ordinals, not the set, because Set iteration order is unspecified.
	public static Element next(Element current, Set<Element> available) {
		Element[] all = values();
		for (int i = 1; i <= all.length; i++) {
			Element candidate = all[(current.ordinal() + i) % all.length];
			if (available.contains(candidate)) return candidate;
		}
		throw new IllegalArgumentException("available must contain STEEL");
	}
}
