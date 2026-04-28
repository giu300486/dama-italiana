package com.damaitaliana.shared.ai.evaluation;

import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.Square;
import java.util.Set;

/**
 * Center control term: rewards pieces on the four central dark squares — FID {@code 14, 15, 18, 19}
 * (SPEC §12.1).
 *
 * <p>In Square coordinates these are {@code (2,4)}, {@code (4,4)}, {@code (3,3)}, {@code (5,3)} —
 * the four dark squares closest to the geometric centre of the board. The term contributes {@code
 * +1} per friendly piece on a centre square and {@code -1} per opponent piece on one (raw "centre
 * piece count" unit). Standard centipawn weight: {@code 10}.
 */
public final class CenterControlTerm implements EvaluationTerm {

  /** The four central dark squares (FID 14, 15, 18, 19). */
  public static final Set<Square> CENTER_SQUARES =
      Set.of(new Square(2, 4), new Square(4, 4), new Square(3, 3), new Square(5, 3));

  @Override
  public int score(GameState state, Color perspective) {
    int total = 0;
    for (Square s : CENTER_SQUARES) {
      Piece p = state.board().at(s).orElse(null);
      if (p == null) {
        continue;
      }
      total += (p.color() == perspective) ? 1 : -1;
    }
    return total;
  }
}
