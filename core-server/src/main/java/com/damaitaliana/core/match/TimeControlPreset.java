package com.damaitaliana.core.match;

/**
 * Canonical time control presets exposed in the UI (SPEC §13.1 setup screens).
 *
 * <p>Each preset carries the canonical {@code initialMillis} / {@code incrementMillis} values,
 * accessible via {@link #asTimeControl()}. Custom overrides remain expressible by constructing a
 * {@link TimeControl} directly.
 */
public enum TimeControlPreset {

  /** 5 minutes initial + 3 seconds per move. */
  BLITZ_5_3(5 * 60_000L, 3_000L),

  /** 15 minutes initial + 10 seconds per move. */
  RAPID_15_10(15 * 60_000L, 10_000L),

  /** 30 minutes initial + 30 seconds per move. */
  CLASSICAL_30_30(30 * 60_000L, 30_000L),

  /** No clock running; play at leisure. */
  UNLIMITED(0L, 0L);

  private final long defaultInitialMillis;
  private final long defaultIncrementMillis;

  TimeControlPreset(long defaultInitialMillis, long defaultIncrementMillis) {
    this.defaultInitialMillis = defaultInitialMillis;
    this.defaultIncrementMillis = defaultIncrementMillis;
  }

  /** Returns a {@link TimeControl} bound to this preset's canonical values. */
  public TimeControl asTimeControl() {
    return new TimeControl(this, defaultInitialMillis, defaultIncrementMillis);
  }

  public long defaultInitialMillis() {
    return defaultInitialMillis;
  }

  public long defaultIncrementMillis() {
    return defaultIncrementMillis;
  }
}
