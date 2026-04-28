package com.damaitaliana.shared.ai.evaluation;

import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Piece;

/**
 * Edge safety term: rewards pieces on the leftmost ({@code file=0}) or rightmost ({@code file=7})
 * columns (SPEC §12.1).
 *
 * <p>Pieces on those files cannot be captured by a diagonal jump — there is no square on the
 * outside to land on. The term contributes {@code +1} per friendly edge piece and {@code -1} per
 * opponent edge piece (raw "edge piece count" unit). Standard centipawn weight: {@code 8}.
 */
public final class EdgeSafetyTerm implements EvaluationTerm {

  @Override
  public int score(GameState state, Color perspective) {
    return state
        .board()
        .occupied()
        .mapToInt(
            s -> {
              if (s.file() != 0 && s.file() != 7) {
                return 0;
              }
              Piece p = state.board().at(s).orElseThrow();
              return (p.color() == perspective) ? 1 : -1;
            })
        .sum();
  }
}
