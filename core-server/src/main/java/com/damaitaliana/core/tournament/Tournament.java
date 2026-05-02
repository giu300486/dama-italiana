package com.damaitaliana.core.tournament;

import com.damaitaliana.core.match.TimeControl;
import com.damaitaliana.core.match.UserRef;
import java.util.List;

/**
 * Tournament aggregate (SPEC §8.3) — sealed over the two formats currently supported: {@link
 * EliminationTournament} (single elimination, Fase 8) and {@link RoundRobinTournament} (Fase 9).
 *
 * <p>Fase 4 ships only the data shape — bracket generation, scheduling, standings, and tie-breaker
 * are deferred to Fase 8/9. The records carry the minimum fields needed by the repository contract
 * and by {@code TournamentEngine.createTournament} (Task 4.10). Richer fields ({@code
 * BracketState}, round-robin schedule, standings, {@code TieBreakerPolicy}) arrive when the
 * relevant logic lands.
 *
 * <p>Note: this package depends on {@code com.damaitaliana.core.match} for {@link UserRef} and
 * {@link TimeControl}; the match package depends back on this one for {@link TournamentMatchRef}.
 * The bidirectional package dependency is documented and reviewable in REVIEW Fase 4 / Task 4.12
 * (ArchUnit).
 */
public sealed interface Tournament permits EliminationTournament, RoundRobinTournament {

  TournamentId id();

  String name();

  TournamentStatus status();

  List<UserRef> participants();

  TimeControl timeControl();
}
