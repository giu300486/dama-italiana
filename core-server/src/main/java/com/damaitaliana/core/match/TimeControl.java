package com.damaitaliana.core.match;

import java.util.Objects;

/**
 * Time control attached as metadata to a match (SPEC §3.7 "Tempo di gioco (online)").
 *
 * <p>In Fase 4 the time control is purely a metadata record — there is no running clock and no
 * timeout enforcement. The clock and the {@code FORFEIT_TIMEOUT} flow arrive in Fase 6 with the
 * server-authoritative transport layer.
 *
 * <p>The {@link TimeControlPreset} hint lets the UI render canonical labels ("Blitz 5+3", "Rapid
 * 15+10"); the {@code initialMillis} / {@code incrementMillis} carry the actual values so a
 * non-canonical override remains expressible.
 */
public record TimeControl(TimeControlPreset preset, long initialMillis, long incrementMillis) {

  public TimeControl {
    Objects.requireNonNull(preset, "preset");
    if (initialMillis < 0) {
      throw new IllegalArgumentException("initialMillis must be >= 0, got: " + initialMillis);
    }
    if (incrementMillis < 0) {
      throw new IllegalArgumentException("incrementMillis must be >= 0, got: " + incrementMillis);
    }
  }

  /**
   * Convenience: a time control with no clock running. Maps to {@link TimeControlPreset#UNLIMITED}.
   */
  public static TimeControl unlimited() {
    return TimeControlPreset.UNLIMITED.asTimeControl();
  }

  /** True iff the preset is {@link TimeControlPreset#UNLIMITED}. */
  public boolean isUnlimited() {
    return preset == TimeControlPreset.UNLIMITED;
  }
}
