package com.damaitaliana.shared.ai.evaluation;

import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;

/**
 * Static heuristic evaluation of a position (SPEC §12.1).
 *
 * <p>The score is in centipawns from {@code perspective}'s point of view: positive means the
 * position favours {@code perspective}, negative means it favours the opponent. {@code 0} means the
 * position is balanced according to the heuristic — it does <em>not</em> mean the game is a draw
 * (terminal status detection lives in the search, not here).
 *
 * <p>Implementations MUST be deterministic and side-effect free.
 */
public interface Evaluator {

  /**
   * Computes the score of {@code state} from the perspective of {@code perspective}.
   *
   * @param state the position to evaluate.
   * @param perspective the colour whose point of view defines the sign.
   * @return centipawn score; positive favours {@code perspective}.
   */
  int evaluate(GameState state, Color perspective);
}
