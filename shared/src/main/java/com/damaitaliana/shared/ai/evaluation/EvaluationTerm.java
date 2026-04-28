package com.damaitaliana.shared.ai.evaluation;

import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;

/**
 * A single component of the heuristic evaluation (SPEC §12.1, ADR-025).
 *
 * <p>A term computes a partial score in its own natural unit (e.g. material returns centipawns
 * directly with weight {@code 1}; mobility returns "extra legal moves" and gets multiplied by a
 * centipawn-per-move weight). Each term is a pure function: same input ⇒ same output, no side
 * effects, no caching.
 *
 * <p>Sign convention: a positive return value MUST favour {@code perspective}; a negative value
 * MUST favour the opponent. The {@link WeightedSumEvaluator} multiplies the result by a (signed)
 * weight before adding it to the total — terms therefore should not pre-apply their own weight.
 */
public interface EvaluationTerm {

  /**
   * Computes the term score of {@code state} from {@code perspective}'s point of view.
   *
   * @param state the position to evaluate.
   * @param perspective the colour whose viewpoint defines the sign.
   * @return raw term score in this term's natural unit.
   */
  int score(GameState state, Color perspective);
}
