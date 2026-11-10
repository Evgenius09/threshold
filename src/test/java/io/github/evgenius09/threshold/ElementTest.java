package io.github.evgenius09.threshold;

import static io.github.evgenius09.threshold.core.Element.SHADOW;
import static io.github.evgenius09.threshold.core.Element.STEEL;
import static io.github.evgenius09.threshold.core.Element.STORM;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.evgenius09.threshold.core.Element;
import java.util.Set;
import org.junit.jupiter.api.Test;

// Numbers come from tech.md 5.1, so a failure here means the code drifted from the spec.
class ElementTest {
	@Test
	void multiplierMatchesTable() {
		// rows: attack STEEL, STORM, SHADOW; columns: defender STEEL, STORM, SHADOW (tech.md 5.1)
		double[][] expected = {
			{1.0, 1.0, 1.0},
			{1.0, 0.5, 1.5},
			{1.0, 1.5, 0.5},
		};
		Element[] order = {STEEL, STORM, SHADOW};
		for (int a = 0; a < 3; a++)
			for (int d = 0; d < 3; d++)
				assertEquals(expected[a][d], Element.multiplier(order[a], order[d]), 1e-9,
						order[a] + " -> " + order[d]);
	}

	@Test
	void colorsMatchSpec() {
		assertEquals(0xC0C8D0, STEEL.color());
		assertEquals(0xFFE04A, STORM.color());
		assertEquals(0x8A3FD1, SHADOW.color());
	}

	@Test
	void nextCyclesThroughAll() {
		Set<Element> all = Set.of(STEEL, STORM, SHADOW);
		assertEquals(STORM, Element.next(STEEL, all));
		assertEquals(SHADOW, Element.next(STORM, all));
		assertEquals(STEEL, Element.next(SHADOW, all));
	}

	@Test
	void nextSkipsMissingElement() {
		Set<Element> steelShadow = Set.of(STEEL, SHADOW);
		assertEquals(SHADOW, Element.next(STEEL, steelShadow));
		assertEquals(STEEL, Element.next(SHADOW, steelShadow));
	}

	@Test
	void nextWithSteelOnlyStaysSteel() {
		assertEquals(STEEL, Element.next(STEEL, Set.of(STEEL)));
	}

	@Test
	void nextSkipsCurrentMissingFromSet() {
		assertEquals(SHADOW, Element.next(STORM, Set.of(STEEL, SHADOW)));
	}
}
