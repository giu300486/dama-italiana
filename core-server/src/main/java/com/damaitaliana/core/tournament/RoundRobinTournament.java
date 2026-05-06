package com.damaitaliana.core.tournament;

import com.damaitaliana.core.match.TimeControl;
import com.damaitaliana.core.match.UserRef;
import java.util.List;
import java.util.Objects;

/**
 * Round-robin tournament (Fase 9). Schedule generation (Berger, single/double), standings, and
 * tie-breaker policy land with {@code RoundRobinScheduler} / {@code TieBreakerPolicy} in Task 4.10
 * / Fase 9. In Fase 4 only the data shape exists.
 */
public record RoundRobinTournament(
    TournamentId id,
    String name,
    TournamentStatus status,
    List<UserRef> participants,
    TimeControl timeControl)
    implements Tournament {

  public RoundRobinTournament {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(participants, "participants");
    Objects.requireNonNull(timeControl, "timeControl");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    participants = List.copyOf(participants);
  }
}
