package com.damaitaliana.shared.ai;

import com.damaitaliana.shared.ai.evaluation.WeightedSumEvaluator;
import com.damaitaliana.shared.ai.search.IterativeDeepeningSearch;
import com.damaitaliana.shared.ai.search.StandardMoveOrderer;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.time.Duration;
import java.util.Objects;

/**
 * Esperto (SPEC §12.2): 5 ply, 2000 ms, alpha-beta + move ordering, no transposition table, no
 * randomness.
 *
 * <p>Built on top of {@link IterativeDeepeningSearch} (rather than a fixed-depth minimax) for
 * graceful handling of cancellation: if the deadline arrives mid-iteration, the engine returns the
 * best move of the deepest completed iteration.
 */
public final class EspertoAi implements AiEngine {

  /** Search depth in ply. */
  public static final int DEPTH = 5;

  /** Default per-move budget; the executor (Task 2.8) translates this to a deadline token. */
  public static final Duration DEFAULT_TIMEOUT = Duration.ofMillis(2000);

  private final IterativeDeepeningSearch search;

  /** Uses a fresh {@link ItalianRuleEngine}. */
  public EspertoAi() {
    this(new ItalianRuleEngine());
  }

  /** Uses an externally provided rule engine — useful for tests with mocks/spies. */
  public EspertoAi(RuleEngine ruleEngine) {
    Objects.requireNonNull(ruleEngine, "ruleEngine");
    this.search = new IterativeDeepeningSearch(ruleEngine);
  }

  @Override
  public Move chooseMove(GameState state, CancellationToken cancel) {
    return search
        .search(
            state,
            DEPTH,
            WeightedSumEvaluator.defaultEvaluator(),
            new StandardMoveOrderer(),
            cancel)
        .bestMove();
  }

  @Override
  public AiLevel level() {
    return AiLevel.ESPERTO;
  }
}
