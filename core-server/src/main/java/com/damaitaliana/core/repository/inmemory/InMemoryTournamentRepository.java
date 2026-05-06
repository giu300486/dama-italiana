package com.damaitaliana.core.repository.inmemory;

import com.damaitaliana.core.repository.TournamentRepository;
import com.damaitaliana.core.tournament.Tournament;
import com.damaitaliana.core.tournament.TournamentId;
import com.damaitaliana.core.tournament.TournamentStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory adapter for {@link TournamentRepository}. Skeleton in Fase 4 (Tournament Engine itself
 * is stubbed until Fase 8/9, see PLAN-fase-4 §4.10): only basic CRUD over a {@link
 * ConcurrentHashMap}. The Fase 6 JPA adapter will replace this for the Internet server.
 */
@Component
public final class InMemoryTournamentRepository implements TournamentRepository {

  private final ConcurrentHashMap<TournamentId, Tournament> snapshots = new ConcurrentHashMap<>();

  @Override
  public Tournament save(Tournament tournament) {
    snapshots.put(tournament.id(), tournament);
    return tournament;
  }

  @Override
  public Optional<Tournament> findById(TournamentId id) {
    return Optional.ofNullable(snapshots.get(id));
  }

  @Override
  public List<Tournament> findByStatus(TournamentStatus status) {
    var tournaments = new ArrayList<Tournament>();
    for (Tournament t : snapshots.values()) {
      if (t.status() == status) {
        tournaments.add(t);
      }
    }
    return List.copyOf(tournaments);
  }
}
