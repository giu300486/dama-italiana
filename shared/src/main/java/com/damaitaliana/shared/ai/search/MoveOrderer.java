package com.damaitaliana.shared.ai.search;

import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import java.util.List;

/**
 * Reorders the moves of a position so that the most promising candidates are searched first (SPEC
 * §12.1).
 *
 * <p>Better ordering ⇒ more alpha-beta cutoffs ⇒ fewer nodes visited at the same depth. The
 * implementation MUST be deterministic — same input list and same state ⇒ same output order.
 */
public interface MoveOrderer {

  /**
   * Returns a new list with the moves of {@code moves} sorted by descending priority. The input
   * list is not modified.
   */
  List<Move> order(List<Move> moves, GameState state);
}
