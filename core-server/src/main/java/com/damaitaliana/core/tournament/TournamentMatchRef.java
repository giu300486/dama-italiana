package com.damaitaliana.core.tournament;

import java.util.Objects;

/**
 * Pointer from a {@link com.damaitaliana.core.match.Match} back to the tournament that scheduled it
 * (SPEC §8.3 — the {@code Match} aggregate carries an {@code Optional<TournamentMatchRef>}).
 *
 * <p>{@code roundNo} is 0-indexed (round 0 is the opening round); {@code matchIndex} is the
 * position within the round (0-indexed). Together with the tournament id they uniquely identify the
 * bracket slot.
 */
public record TournamentMatchRef(TournamentId tournamentId, int roundNo, int matchIndex) {

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
