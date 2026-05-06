package com.damaitaliana.core.tournament;

import com.damaitaliana.core.match.TimeControl;
import com.damaitaliana.core.match.UserRef;
import java.util.List;
import java.util.Objects;

/**
 * Single-elimination tournament (Fase 8). Bracket generation, seeding, byes, and round progression
 * land with {@code BracketGenerator} in Task 4.10 / Fase 8. In Fase 4 only the data shape exists.
 */
public record EliminationTournament(
    TournamentId id,
    String name,
    TournamentStatus status,
    List<UserRef> participants,
    TimeControl timeControl)
    implements Tournament {

  public EliminationTournament {
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
