package com.damaitaliana.shared.ai.search;

import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link MoveOrderer} decorator that puts the principal-variation move (the bestMove of the
 * previous iterative-deepening iteration) at index {@code 0} of the resulting list, then defers to
 * the wrapped base orderer for everything else.
 *
 * <p>If the PV move is not in the input list (e.g. because the search recursed into a state where
 * the PV is illegal), the decorator is a no-op pass-through.
 *
 * <p>Package-private: this is an IDS implementation detail and exposing it widely would tempt
 * misuse outside the iterative-deepening context.
 */
final class PvFirstOrderer implements MoveOrderer {

  private final MoveOrderer base;
  private final Move pv;

  PvFirstOrderer(MoveOrderer base, Move pv) {
    this.base = base;
    this.pv = pv;
  }

  @Override
  public List<Move> order(List<Move> moves, GameState state) {
    List<Move> baseOrder = base.order(moves, state);
    if (pv == null || !baseOrder.contains(pv)) {
      return baseOrder;
    }
    List<Move> result = new ArrayList<>(baseOrder.size());
    result.add(pv);
    for (Move m : baseOrder) {
      if (!m.equals(pv)) {
        result.add(m);
      }
    }
    return List.copyOf(result);
  }
}
