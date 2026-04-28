package com.damaitaliana.client.controller;

import com.damaitaliana.shared.domain.Color;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * User-facing colour selection in the single-player setup screen. Adds {@link #RANDOM} on top of
 * the two concrete {@link Color} values; {@link #resolve(RandomGenerator)} maps it to {@code
 * Color.WHITE} or {@code Color.BLACK} via a coin flip on the supplied generator so the choice is
 * deterministic when the caller passes a seeded RNG (used by tests).
 */
public enum ColorChoice {
  WHITE,
  BLACK,
  RANDOM;

  public Color resolve(RandomGenerator rng) {
    Objects.requireNonNull(rng, "rng");
    return switch (this) {
      case WHITE -> Color.WHITE;
      case BLACK -> Color.BLACK;
      case RANDOM -> rng.nextBoolean() ? Color.WHITE : Color.BLACK;
    };
  }
}
