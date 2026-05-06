package com.damaitaliana.core.match;

import java.util.Objects;
import java.util.UUID;

/**
 * Opaque identifier for a match.
 *
 * <p>Wraps a {@link UUID} (random in production via {@link #random()}, deterministic from a string
 * in tests via {@link #of(String)}). The wrapping prevents accidental mixing with {@code
 * com.damaitaliana.core.tournament.TournamentId}, which is also a UUID-backed identifier.
 *
 * <p>Wire shape (FR-COM-02): default Jackson serialization yields {@code {"value":"uuid-string"}}.
 * The transport layer (Fase 6 server, Fase 7 client) installs a custom Jackson {@code Module} to
 * collapse this to a bare scalar string per SPEC §11.4.
 */
public record MatchId(UUID value) {

  public MatchId {
    Objects.requireNonNull(value, "value");
  }

  /** Returns a new {@code MatchId} backed by a random UUID v4. */
  public static MatchId random() {
    return new MatchId(UUID.randomUUID());
  }

  /**
   * Parses {@code text} as a UUID and wraps it.
   *
   * @throws IllegalArgumentException if {@code text} is not a valid UUID.
   */
  public static MatchId of(String text) {
    return new MatchId(UUID.fromString(text));
  }
}
