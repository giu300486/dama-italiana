package com.damaitaliana.shared.ai;

import com.damaitaliana.shared.ai.evaluation.WeightedSumEvaluator;
import com.damaitaliana.shared.ai.search.IterativeDeepeningSearch;
import com.damaitaliana.shared.ai.search.SearchResult;
import com.damaitaliana.shared.ai.search.StandardMoveOrderer;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Principiante (SPEC §12.2): 2 ply, 500 ms, 25 % probability of picking a sub-optimal move.
 *
 * <p>The engine first runs an iterative-deepening search up to {@link #DEPTH}. If the resulting
 * best-move and the root has more than one legal move, with probability {@link #NOISE_PROBABILITY}
 * the engine swaps the best for a uniformly chosen <em>non</em>-best legal move. With one legal
 * move (forced capture, single escape) noise is bypassed (ADR-028).
 */
public final class PrincipianteAi implements AiEngine {

  /** Search depth in ply. */
  public static final int DEPTH = 2;

  /** Default per-move budget; the executor (Task 2.8) translates this to a deadline token. */
  public static final Duration DEFAULT_TIMEOUT = Duration.ofMillis(500);

  /** Probability of selecting a sub-optimal move when more than one legal move is available. */
  public static final double NOISE_PROBABILITY = 0.25;

  private final RuleEngine ruleEngine;
  private final IterativeDeepeningSearch search;
  private final RandomGenerator rng;

  /** Uses a fresh {@link ItalianRuleEngine}. */
  public PrincipianteAi(RandomGenerator rng) {
    this(new ItalianRuleEngine(), rng);
  }

  /** Uses an externally provided rule engine — useful for tests with mocks/spies. */
  public PrincipianteAi(RuleEngine ruleEngine, RandomGenerator rng) {
    this.ruleEngine = Objects.requireNonNull(ruleEngine, "ruleEngine");
    this.search = new IterativeDeepeningSearch(ruleEngine);
    this.rng = Objects.requireNonNull(rng, "rng");
  }

  @Override
  public Move chooseMove(GameState state, CancellationToken cancel) {
    SearchResult result =
        search.search(
            state,
            DEPTH,
            WeightedSumEvaluator.defaultEvaluator(),
            new StandardMoveOrderer(),
            cancel);
    Move best = result.bestMove();
    if (best == null) {
      return null; // terminal state
    }
    List<Move> legal = ruleEngine.legalMoves(state);
    if (legal.size() <= 1) {
      return best;
    }
    if (rng.nextDouble() < NOISE_PROBABILITY) {
      Move alternative;
      do {
        alternative = legal.get(rng.nextInt(legal.size()));
      } while (alternative.equals(best));
      return alternative;
    }
    return best;
  }

  @Override
  public AiLevel level() {
    return AiLevel.PRINCIPIANTE;
  }
}
