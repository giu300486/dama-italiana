package com.damaitaliana.shared.ai.search;

import com.damaitaliana.shared.ai.evaluation.CenterControlTerm;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.notation.FidNotation;
import java.util.Comparator;
import java.util.List;

/**
 * Default {@link MoveOrderer} implementation following SPEC §12.1 priorities, in this order:
 *
 * <ol>
 *   <li>Captures before simple moves. (Mostly redundant: in Italian draughts the four laws of
 *       precedence already make the legal-move list either all-captures or all-simple, but the
 *       comparator stays correct for hand-built move lists in tests.)
 *   <li>Among captures, longer sequences first (proxy for the quantity law).
 *   <li>Among simple moves with equal capture rank, moves whose destination is one of the four
 *       centre squares ({@link CenterControlTerm#CENTER_SQUARES}) first.
 *   <li>Stable tie-break: the FID number of {@code from} ascending.
 * </ol>
 */
public final class StandardMoveOrderer implements MoveOrderer {

  private static final Comparator<Move> COMPARATOR =
      Comparator.<Move>comparingInt(m -> m.isCapture() ? 0 : 1)
          .thenComparingInt(m -> -m.capturedSquares().size())
          .thenComparingInt(m -> CenterControlTerm.CENTER_SQUARES.contains(m.to()) ? 0 : 1)
          .thenComparingInt(m -> FidNotation.toFid(m.from()));

  @Override
  public List<Move> order(List<Move> moves, GameState state) {
    return moves.stream().sorted(COMPARATOR).toList();
  }
}
