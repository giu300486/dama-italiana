package com.damaitaliana.core.repository;

import com.damaitaliana.core.tournament.Tournament;
import com.damaitaliana.core.tournament.TournamentId;
import com.damaitaliana.core.tournament.TournamentStatus;
import java.util.List;
import java.util.Optional;

/**
 * Port for tournament persistence. Skeleton in Fase 4: only basic CRUD; richer queries (by
 * participant, by deadline, by format) arrive with the tournament logic in Fase 8/9.
 *
 * <p>Constraint (CLAUDE.md §8.8): plain Java interface — no JPA. The JPA adapter lives in the
 * {@code server} module (Fase 6+).
 */
public interface TournamentRepository {

  /** Persists or updates the tournament snapshot. */
  Tournament save(Tournament tournament);

  /** Look up by id. */
  Optional<Tournament> findById(TournamentId id);

  /** Look up by status (e.g. {@code findByStatus(IN_PROGRESS)} for active tournaments). */
  List<Tournament> findByStatus(TournamentStatus status);
}
