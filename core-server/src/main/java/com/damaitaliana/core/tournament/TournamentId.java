package com.damaitaliana.core.tournament;

import java.util.Objects;
import java.util.UUID;

/**
 * Opaque identifier for a tournament (SPEC §8.4 column {@code tournaments.external_id}).
 * UUID-backed to avoid mixing with {@link com.damaitaliana.core.match.MatchId}.
 */
public record TournamentId(UUID value) {

  public TournamentId {
    Objects.requireNonNull(value, "value");
  }

  /** Returns a new {@code TournamentId} backed by a random UUID v4. */
  public static TournamentId random() {
    return new TournamentId(UUID.randomUUID());
  }

  /**
   * Parses {@code text} as a UUID and wraps it.
   *
   * @throws IllegalArgumentException if {@code text} is not a valid UUID.
   */
  public static TournamentId of(String text) {
    return new TournamentId(UUID.fromString(text));
  }
}
