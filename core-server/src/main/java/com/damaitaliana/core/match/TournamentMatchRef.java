package com.damaitaliana.core.match;

import java.util.Objects;
import java.util.UUID;

/**
 * Pointer from a {@link Match} back to the tournament that scheduled it (SPEC §8.3 — the {@code
 * Match} aggregate carries an {@code Optional<TournamentMatchRef>}).
 *
 * <p>{@code roundNo} is 0-indexed (round 0 is the opening round); {@code matchIndex} is the
 * position within the round (0-indexed). Together with the tournament id they uniquely identify the
 * bracket slot.
 *
 * <p><b>Package placement</b> (CR Task 4.12 Option F): this back-pointer is match-side metadata, so
 * it lives in the {@code match} package; the tournament identifier is stored as a raw {@link UUID}
 * rather than as {@code TournamentId} so the {@code match} package does not depend on {@code
 * tournament} (PLAN-fase-4 §4.12 sub-package layering rule, ArchUnit-enforced via {@code
 * CoreServerArchitectureTest}). Callers needing the typed {@code TournamentId} wrap the UUID at the
 * lookup site: {@code new TournamentId(ref.tournamentId())}. This is a deliberate ID-only
 * cross-aggregate reference (DDD pattern) and matches how Fase 6 wire serialization will round-trip
 * the field.
 */
public record TournamentMatchRef(UUID tournamentId, int roundNo, int matchIndex) {

  public TournamentMatchRef {
    Objects.requireNonNull(tournamentId, "tournamentId");
    if (roundNo < 0) {
      throw new IllegalArgumentException("roundNo must be >= 0, got: " + roundNo);
    }
    if (matchIndex < 0) {
      throw new IllegalArgumentException("matchIndex must be >= 0, got: " + matchIndex);
    }
  }
}
