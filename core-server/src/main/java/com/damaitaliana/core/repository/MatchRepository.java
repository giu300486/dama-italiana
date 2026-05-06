package com.damaitaliana.core.repository;

import com.damaitaliana.core.match.Match;
import com.damaitaliana.core.match.MatchId;
import com.damaitaliana.core.match.MatchStatus;
import com.damaitaliana.core.match.UserRef;
import com.damaitaliana.core.match.event.MatchEvent;
import java.util.List;
import java.util.Optional;

/**
 * Port for match persistence. Implementations live in adapters: in-memory (Task 4.4, used by the
 * LAN host and core-server tests) and JPA (Fase 6, in the {@code server} module).
 *
 * <p>The contract enforces an append-only event log per match with a strictly monotonic sequence
 * number (FR-COM-04, SPEC §7.5). The {@link Match} aggregate snapshot is also persisted for fast
 * lookup by id, status, or participant.
 *
 * <p>Constraint (CLAUDE.md §8.8): this port MUST NOT reference JPA, Hibernate, or any persistence
 * framework — it is a plain Java interface. The {@code server} module wires it to JPA via its own
 * adapter classes in Fase 6.
 */
public interface MatchRepository {

  /**
   * Persists or updates the {@link Match} snapshot. Implementations MAY return the same instance
   * (in-memory) or a managed entity (JPA, Fase 6). Mutation of the returned reference is visible to
   * subsequent {@link #findById(MatchId)} lookups in the in-memory adapter.
   */
  Match save(Match match);

  /** Look up by id. Returns empty if no match with that id has been saved. */
  Optional<Match> findById(MatchId id);

  /**
   * Look up by status — useful for lobby listings ({@code findByStatus(WAITING)}). Returns matches
   * in unspecified order; callers MUST sort if a deterministic order is required.
   */
  List<Match> findByStatus(MatchStatus status);

  /**
   * Look up by participant — returns matches where the user is either the white or the black
   * player. Useful for player history (SPEC §11.2 {@code GET /users/me/matches}).
   *
   * <p>Equality is by {@link UserRef#id()} (and username for anonymous LAN users): two {@link
   * UserRef} values that compare equal via {@link Object#equals(Object)} match the same matches.
   */
  List<Match> findByPlayer(UserRef user);

  /**
   * Atomically appends an event to the match's log. The caller (typically {@code MatchManager},
   * Task 4.8) MUST set {@code event.sequenceNo()} to {@code currentSequenceNo(event.matchId()) + 1}
   * — implementations validate strict monotonicity and throw {@link IllegalStateException} on
   * violation (FR-COM-04). Per-match write serialization is the caller's responsibility (the
   * MatchManager uses an internal lock per match).
   *
   * @throws IllegalStateException if the supplied sequence number is not exactly {@code
   *     currentSequenceNo(matchId) + 1}.
   */
  void appendEvent(MatchEvent event);

  /**
   * Returns the events of {@code matchId} with {@code sequenceNo > fromSeq}, in ascending sequence
   * order. Used for replay on reconnection (FR-COM-04, AC §17.1.11). Pass {@code -1L} to fetch the
   * entire log. Returns an empty list if the match is unknown or has no events past the cursor.
   */
  List<MatchEvent> eventsSince(MatchId matchId, long fromSeq);

  /**
   * Returns the highest sequence number assigned for {@code matchId}, or {@code -1} if no events
   * have been appended yet. Useful for callers preparing the next event: {@code newSeq =
   * repo.currentSequenceNo(mid) + 1}.
   */
  long currentSequenceNo(MatchId matchId);
}
