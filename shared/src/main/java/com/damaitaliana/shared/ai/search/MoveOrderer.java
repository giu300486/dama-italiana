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
   *
   * <p>The {@code state} parameter is supplied so context-aware orderers (e.g. ones that prefer
   * captures of advanced enemy pieces, or that consult a positional opening book) can use it. The
   * shipped {@link StandardMoveOrderer} ignores it: its priorities derive entirely from {@link
   * Move} properties. Implementations that don't need the state are free to discard it
   * (REVIEW-fase-2 finding F-005).
   */
  List<Move> order(List<Move> moves, GameState state);
}
