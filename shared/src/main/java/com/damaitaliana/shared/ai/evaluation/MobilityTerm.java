package com.damaitaliana.shared.ai.evaluation;

import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.util.Objects;

/**
 * Mobility term: difference between the number of legal moves available to {@code perspective} and
 * those available to the opponent (SPEC §12.1).
 *
 * <p>Returned in raw "extra moves count" units. The standard centipawn weight is {@code 5}, applied
 * by {@link WeightedSumEvaluator}.
 *
 * <p>Implementation note: the term has to compute legal moves from both viewpoints, which means
 * synthesising a sibling {@link GameState} with the opposite {@code sideToMove}. The synthesised
 * state always carries {@link GameStatus#ONGOING} — terminal states are handled by the search, not
 * by the evaluator.
 */
public final class MobilityTerm implements EvaluationTerm {

  private final RuleEngine ruleEngine;

  /** Uses a fresh {@link ItalianRuleEngine}. Suitable for the default evaluator. */
  public MobilityTerm() {
    this(new ItalianRuleEngine());
  }

  /** Uses an externally provided rule engine — useful for tests with mocks/spies. */
  public MobilityTerm(RuleEngine ruleEngine) {
    this.ruleEngine = Objects.requireNonNull(ruleEngine, "ruleEngine");
  }

  @Override
  public int score(GameState state, Color perspective) {
    int mine = legalMoveCount(state, perspective);
    int theirs = legalMoveCount(state, perspective.opposite());
    return mine - theirs;
  }

  private int legalMoveCount(GameState state, Color sideToMove) {
    GameState perspectiveState =
        new GameState(
            state.board(), sideToMove, state.halfmoveClock(), state.history(), GameStatus.ONGOING);
    return ruleEngine.legalMoves(perspectiveState).size();
  }
}
