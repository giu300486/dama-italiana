package com.damaitaliana.core.tournament;

import com.damaitaliana.core.match.UserRef;
import java.util.Optional;

/**
 * Orchestrator port for the tournament lifecycle (SPEC §8.3, PLAN-fase-4 §4.10). Fase 4 ships only
 * the contract plus the registration phase ({@link #createTournament}, {@link
 * #registerParticipant}); the start phase ({@link #startTournament}) and the underlying bracket
 * generation / scheduling are deferred to Fase 8 (single elimination) and Fase 9 (round robin) and
 * throw {@link UnsupportedOperationException} until then.
 */
public interface TournamentEngine {

  /**
   * Creates a tournament from {@code spec} with status {@link TournamentStatus#CREATED} and an
   * empty participant list. The engine assigns a fresh {@link TournamentId}; callers are expected
   * to share it with prospective participants via the F6 lobby UI (out of scope for Fase 4).
   */
  Tournament createTournament(TournamentSpec spec);

  /**
   * Registers {@code participant} for the tournament identified by {@code id}. Returns the updated
   * tournament. Implementations must reject double registrations and tournaments past status {@link
   * TournamentStatus#CREATED}. Roster-cap enforcement (the {@code maxParticipants} field on {@link
   * TournamentSpec}) is deferred to Fase 8 start-time validation — the {@link Tournament} aggregate
   * records the participant list but not the cap (SPEC §8.3 data shape).
   *
   * @throws java.util.NoSuchElementException if {@code id} does not match any tournament.
   * @throws IllegalStateException if registration is closed (status != CREATED).
   * @throws IllegalArgumentException if {@code participant} is already registered.
   */
  Tournament registerParticipant(TournamentId id, UserRef participant);

  /**
   * Starts the tournament: generates the bracket / schedule and transitions status to {@link
   * TournamentStatus#IN_PROGRESS}. Deferred to Fase 8/9 — the Fase 4 implementation throws {@link
   * UnsupportedOperationException}.
   */
  Tournament startTournament(TournamentId id);

  /** Lookup pass-through to {@link com.damaitaliana.core.repository.TournamentRepository}. */
  Optional<Tournament> findById(TournamentId id);
}
