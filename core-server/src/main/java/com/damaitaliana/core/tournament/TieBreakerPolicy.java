package com.damaitaliana.core.tournament;

import com.damaitaliana.core.match.UserRef;
import java.util.List;

/**
 * Tie-breaker policy for round-robin standings (SPEC §8.3, Fase 9). When two or more participants
 * end the tournament with the same score, this policy resolves the tie into a strict ordering using
 * the {@link RoundRobinTournament} state (head-to-head, Sonneborn-Berger, etc.).
 *
 * <p>Fase 4 ships only the contract plus a no-op fallback ({@link NoOpTieBreakerPolicy}). The Fase
 * 9 implementation will replace it with a Buchholz-style policy in line with Italian-draughts
 * federation practice (decision deferred to the Fase 9 PLAN).
 */
public interface TieBreakerPolicy {

  /**
   * Returns the {@code tied} participants in resolved order. Implementations must preserve every
   * input participant exactly once in the output; the order may differ. {@code tournament} provides
   * the standings context (results, head-to-head, opponent strength) needed by the resolution
   * rules.
   */
  List<UserRef> resolveTies(List<UserRef> tied, RoundRobinTournament tournament);
}
