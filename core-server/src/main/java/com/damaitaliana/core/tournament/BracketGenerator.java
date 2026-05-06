package com.damaitaliana.core.tournament;

import com.damaitaliana.core.match.UserRef;
import java.util.List;

/**
 * Single-elimination bracket generator (SPEC §8.3, Fase 8). Given an ordered list of seeds, returns
 * the round-by-round bracket structure including byes for non-power-of-two participant counts.
 *
 * <p>Fase 4 ships only the contract; the real implementation lands in Fase 8. The Fase 4 stub
 * throws {@link UnsupportedOperationException} — see PLAN-fase-4 §4.10 risk R-2.
 */
public interface BracketGenerator {

  /**
   * Generates the bracket for the given {@code seeds}. The contract is intentionally minimal in
   * Fase 4: the return type is the raw list of pairings the Fase 8 implementation will compute. A
   * richer {@code BracketState} type (with rounds, byes, advancement) will be introduced when the
   * generator is implemented for real.
   */
  List<UserRef> generate(List<UserRef> seeds);
}
