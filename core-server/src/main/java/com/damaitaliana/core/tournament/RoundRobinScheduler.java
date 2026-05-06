package com.damaitaliana.core.tournament;

import com.damaitaliana.core.match.UserRef;
import java.util.List;

/**
 * Round-robin schedule generator (SPEC §8.3, Fase 9). Given a participant list, produces the full
 * Berger-table schedule (single round-robin: every player meets every other player exactly once).
 *
 * <p>Fase 4 ships only the contract; the real implementation lands in Fase 9. The Fase 4 stub
 * throws {@link UnsupportedOperationException} — see PLAN-fase-4 §4.10 risk R-2.
 */
public interface RoundRobinScheduler {

  /**
   * Builds the round-robin schedule for {@code participants}. The Fase 4 contract returns the raw
   * participant list shape; a richer {@code RoundRobinMatch} record (round number, white, black,
   * bye marker) will arrive with the Fase 9 implementation.
   */
  List<UserRef> schedule(List<UserRef> participants);
}
