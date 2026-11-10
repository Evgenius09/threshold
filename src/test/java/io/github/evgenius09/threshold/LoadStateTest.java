package io.github.evgenius09.threshold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.evgenius09.threshold.core.LoadState;
import org.junit.jupiter.api.Test;

// Numbers come from tech.md 5.6, so a failure here means the code drifted from the spec.
class LoadStateTest {
	private static LoadState ticks(LoadState s, int n) {
		for (int i = 0; i < n; i++) s = s.tick();
		return s;
	}

	@Test
	void oneSwitchAddsCost() {
		LoadState s = LoadState.EMPTY.afterSwitch();
		assertEquals(25.0, s.load(), 1e-9);
		assertFalse(s.rejecting());
		assertEquals(0, s.rejectionTicks());
	}

	@Test
	void fourSwitchesStartRejection() {
		LoadState s = LoadState.EMPTY.afterSwitch().afterSwitch().afterSwitch().afterSwitch();
		assertEquals(100.0, s.load(), 1e-9);
		assertEquals(160, s.rejectionTicks());
		assertTrue(s.rejecting());
	}

	@Test
	void switchFromNinetyClampsAndRejects() {
		LoadState s = new LoadState(90.0, 0).afterSwitch();
		assertEquals(100.0, s.load(), 1e-9);
		assertEquals(160, s.rejectionTicks());
	}

	@Test
	void switchDuringRejectionIsIgnored() {
		LoadState s = new LoadState(100.0, 50);
		assertSame(s, s.afterSwitch());
	}

	@Test
	void decayTakesFivePerSecond() {
		LoadState start = new LoadState(25.0, 0);
		assertEquals(20.0, ticks(start, 20).load(), 1e-9);
		assertEquals(0.0, ticks(start, 100).load(), 1e-9);
		LoadState after = ticks(start, 101);
		assertEquals(0.0, after.load(), 1e-9);
		assertFalse(after.rejecting());
	}

	@Test
	void rejectionHoldsMaxThenClears() {
		LoadState start = new LoadState(100.0, 160);
		LoadState almost = ticks(start, 159);
		assertEquals(100.0, almost.load(), 1e-9);
		assertTrue(almost.rejecting());
		assertEquals(1, almost.rejectionTicks());
		LoadState done = ticks(start, 160);
		assertEquals(0.0, done.load(), 1e-9);
		assertFalse(done.rejecting());
		assertEquals(0, done.rejectionTicks());
	}

	@Test
	void decayFromFractionStopsAtZero() {
		assertEquals(0.0, new LoadState(0.1, 0).tick().load(), 1e-9);
	}

	@Test
	void tickOnEmptyStaysEmpty() {
		assertEquals(LoadState.EMPTY, LoadState.EMPTY.tick());
	}
}
