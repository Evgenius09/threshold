package io.github.evgenius09.threshold.core;

public record LoadState(double load, int rejectionTicks) {
	public static final double MAX = 100.0;
	public static final double SWITCH_COST = 25.0;
	public static final double DECAY_PER_TICK = 0.25;
	public static final int REJECTION_TICKS = 160;
	public static final LoadState EMPTY = new LoadState(0.0, 0);

	public boolean rejecting() {
		return rejectionTicks > 0;
	}

	public LoadState afterSwitch() {
		if (rejecting()) return this;
		double next = Math.min(MAX, load + SWITCH_COST);
		return new LoadState(next, next == MAX ? REJECTION_TICKS : 0);
	}

	// Load stays at MAX during rejection so the HUD bar reads full until it ends (tech.md 5.6).
	public LoadState tick() {
		if (rejecting()) {
			int left = rejectionTicks - 1;
			return new LoadState(left == 0 ? 0.0 : MAX, left);
		}
		return new LoadState(Math.max(0.0, load - DECAY_PER_TICK), 0);
	}
}
