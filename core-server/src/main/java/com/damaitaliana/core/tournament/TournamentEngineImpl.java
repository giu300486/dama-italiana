package com.damaitaliana.core.tournament;

import com.damaitaliana.core.match.UserRef;
import com.damaitaliana.core.repository.TournamentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Fase 4 skeleton {@link TournamentEngine} (PLAN-fase-4 §4.10). Implements the registration phase
 * ({@link #createTournament}, {@link #registerParticipant}) and stubs the start phase. Bracket
 * generation arrives with Fase 8, round-robin scheduling/standings with Fase 9 (PLAN risk R-2).
 *
 * <p>Concurrency: a per-tournament intrinsic lock serializes writes so {@link #registerParticipant}
 * cannot lose updates under contention. {@link #findById} is lock-free and returns the live
 * snapshot from the repository.
 */
@Service
public final class TournamentEngineImpl implements TournamentEngine {

  private final TournamentRepository repo;
  private final ConcurrentHashMap<TournamentId, Object> locks = new ConcurrentHashMap<>();

  public TournamentEngineImpl(TournamentRepository repo) {
    this.repo = Objects.requireNonNull(repo, "repo");
  }

  @Override
  public Tournament createTournament(TournamentSpec spec) {
    Objects.requireNonNull(spec, "spec");
    TournamentId id = TournamentId.random();
    Tournament tournament =
        switch (spec.format()) {
          case SINGLE_ELIMINATION ->
              new EliminationTournament(
                  id, spec.name(), TournamentStatus.CREATED, List.of(), spec.timeControl());
          case ROUND_ROBIN ->
              new RoundRobinTournament(
                  id, spec.name(), TournamentStatus.CREATED, List.of(), spec.timeControl());
        };
    repo.save(tournament);
    return tournament;
  }

  @Override
  public Tournament registerParticipant(TournamentId id, UserRef participant) {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(participant, "participant");

    synchronized (lockFor(id)) {
      Tournament current =
          repo.findById(id)
              .orElseThrow(() -> new NoSuchElementException("No tournament with id " + id));

      if (current.status() != TournamentStatus.CREATED) {
        throw new IllegalStateException(
            "Cannot register on tournament "
                + id
                + ": registration closed, status="
                + current.status());
      }
      if (current.participants().contains(participant)) {
        throw new IllegalArgumentException(
            "Participant " + participant + " is already registered for tournament " + id);
      }

      var roster = new ArrayList<>(current.participants());
      roster.add(participant);
      Tournament updated = withParticipants(current, roster);
      repo.save(updated);
      return updated;
    }
  }

  @Override
  public Tournament startTournament(TournamentId id) {
    Objects.requireNonNull(id, "id");
    throw new UnsupportedOperationException(
        "TournamentEngine.startTournament is deferred to Fase 8 (single-elimination) / Fase 9"
            + " (round-robin). PLAN-fase-4 §4.10 risk R-2.");
  }

  @Override
  public Optional<Tournament> findById(TournamentId id) {
    Objects.requireNonNull(id, "id");
    return repo.findById(id);
  }

  // --- Helpers --------------------------------------------------------------

  private Object lockFor(TournamentId id) {
    return locks.computeIfAbsent(id, k -> new Object());
  }

  private static Tournament withParticipants(Tournament current, List<UserRef> roster) {
    return switch (current) {
      case EliminationTournament e ->
          new EliminationTournament(e.id(), e.name(), e.status(), roster, e.timeControl());
      case RoundRobinTournament r ->
          new RoundRobinTournament(r.id(), r.name(), r.status(), roster, r.timeControl());
    };
  }
}
