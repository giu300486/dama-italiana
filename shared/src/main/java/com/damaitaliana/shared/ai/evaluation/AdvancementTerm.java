package com.damaitaliana.shared.ai.evaluation;

import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;

/**
 * Advancement term: how far each MAN has progressed towards its promotion row (SPEC §12.1).
 *
 * <p>White's promotion row is rank 7, so a white man on rank {@code r} contributes {@code r}; black
 * promotes on rank 0, so a black man on rank {@code r} contributes {@code 7 - r}. Kings are
 * <em>not</em> counted — they have already been promoted and the term does not double-credit them.
 *
 * <p>Returned in raw "rank steps" units. The standard centipawn weight is {@code 2}, applied by
 * {@link WeightedSumEvaluator}.
 */
public final class AdvancementTerm implements EvaluationTerm {

  @Override
  public int score(GameState state, Color perspective) {
    return state
        .board()
        .occupied()
        .mapToInt(
            s -> {
              Piece p = state.board().at(s).orElseThrow();
              if (p.kind() != PieceKind.MAN) {
                return 0;
              }
              int advancement = (p.color() == Color.WHITE) ? s.rank() : (7 - s.rank());
              return (p.color() == perspective) ? advancement : -advancement;
            })
        .sum();
  }
}
