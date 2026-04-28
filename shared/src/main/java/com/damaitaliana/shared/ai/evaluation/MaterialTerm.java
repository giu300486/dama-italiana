package com.damaitaliana.shared.ai.evaluation;

import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;

/**
 * Material balance — the most fundamental evaluation term.
 *
 * <p>Counts each piece directly in centipawns: a man is worth {@value #MAN_VALUE} cp, a king
 * {@value #KING_VALUE} cp (SPEC §12.1). Composed at weight {@code 1} inside {@link
 * WeightedSumEvaluator#defaultEvaluator()}.
 */
public final class MaterialTerm implements EvaluationTerm {

  /** Centipawn value of a man (pedina), per SPEC §12.1. */
  public static final int MAN_VALUE = 100;

  /** Centipawn value of a king (dama), per SPEC §12.1. */
  public static final int KING_VALUE = 300;

  @Override
  public int score(GameState state, Color perspective) {
    return state
        .board()
        .occupied()
        .mapToInt(
            s -> {
              Piece p = state.board().at(s).orElseThrow();
              int v = (p.kind() == PieceKind.KING) ? KING_VALUE : MAN_VALUE;
              return (p.color() == perspective) ? v : -v;
            })
        .sum();
  }
}
