package com.damaitaliana.core.tournament;

import com.damaitaliana.core.match.TimeControl;
import java.util.Objects;

/**
 * Input parameter for {@link TournamentEngine#createTournament(TournamentSpec)} (PLAN-fase-4
 * §4.10). Captures the user-facing choice of name, format, time control, and registration cap; the
 * engine builds the matching {@link Tournament} variant from these fields.
 *
 * <p>{@code maxParticipants} is enforced at registration time by the engine. A value of {@code 0}
 * is rejected: there is no meaningful tournament with no participants and SPEC §8.3
 * single-elimination brackets need at least two seats. The maximum is left to the engine to
 * validate against format-specific rules in Fase 8/9 (e.g. powers of two for clean
 * single-elimination).
 */
public record TournamentSpec(
    String name, TournamentFormat format, TimeControl timeControl, int maxParticipants) {

  public TournamentSpec {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(format, "format");
    Objects.requireNonNull(timeControl, "timeControl");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (maxParticipants < 2) {
      throw new IllegalArgumentException("maxParticipants must be >= 2, got: " + maxParticipants);
    }
  }
}
