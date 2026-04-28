package com.damaitaliana.shared.ai.search;

import com.damaitaliana.shared.ai.CancellationToken;
import com.damaitaliana.shared.ai.SearchCancelledException;
import com.damaitaliana.shared.ai.evaluation.Evaluator;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.util.List;
import java.util.Objects;

/**
 * Iterative deepening on top of {@link MinimaxSearch} (SPEC §12.1).
 *
 * <p>Calls the underlying negamax at depths {@code 1, 2, 3, …, maxDepth} and keeps the best move of
 * the deepest <em>completed</em> iteration. If cancellation arrives mid-iteration {@code k}, the
 * search returns the best move of iteration {@code k-1}; if not even iteration {@code 1} managed to
 * finish, it returns the first legal move of {@code state} as a desperate fallback so callers never
 * have to deal with {@code null}.
 *
 * <p>Move ordering at each iteration after the first puts the previous bestMove first ({@link
 * PvFirstOrderer}) so alpha-beta cutoffs trigger sooner. When a forced mate is found the search
 * stops early — going deeper cannot improve a mate score.
 */
public final class IterativeDeepeningSearch {

  /** Mate-score threshold used to detect "any forced mate" results regardless of plies-to-mate. */
  private static final int MATE_THRESHOLD = MinimaxSearch.MATE_SCORE - 1000;

  private final RuleEngine ruleEngine;
  private final MinimaxSearch minimax;

  /** Uses a fresh {@link ItalianRuleEngine} for both the fallback and the underlying minimax. */
  public IterativeDeepeningSearch() {
    this(new ItalianRuleEngine());
  }

  /** Uses an externally provided rule engine for both the fallback and the underlying minimax. */
  public IterativeDeepeningSearch(RuleEngine ruleEngine) {
    this.ruleEngine = Objects.requireNonNull(ruleEngine, "ruleEngine");
    this.minimax = new MinimaxSearch(ruleEngine);
  }

  /**
   * Iteratively deepens the search up to {@code maxDepth}.
   *
   * @return the result of the deepest completed iteration, or a fallback if even depth {@code 1}
   *     was cancelled. The {@link SearchResult#bestMove()} is non-{@code null} unless the root
   *     state is already terminal.
   * @throws IllegalArgumentException if {@code maxDepth < 1}.
   */
  public SearchResult search(
      GameState state,
      int maxDepth,
      Evaluator evaluator,
      MoveOrderer baseOrderer,
      CancellationToken cancel) {
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(evaluator, "evaluator");
    Objects.requireNonNull(baseOrderer, "baseOrderer");
    Objects.requireNonNull(cancel, "cancel");
    if (maxDepth < 1) {
      throw new IllegalArgumentException("maxDepth must be >= 1: " + maxDepth);
    }

    SearchResult best = null;
    Move pv = null;
    long totalNodes = 0;

    for (int depth = 1; depth <= maxDepth; depth++) {
      MoveOrderer orderer = (pv == null) ? baseOrderer : new PvFirstOrderer(baseOrderer, pv);
      try {
        SearchResult iteration = minimax.search(state, depth, evaluator, orderer, cancel);
        totalNodes += iteration.nodesVisited();
        best = new SearchResult(iteration.bestMove(), iteration.score(), depth, totalNodes);
        pv = iteration.bestMove();
        if (Math.abs(iteration.score()) >= MATE_THRESHOLD) {
          break; // mate found; deeper search cannot improve.
        }
      } catch (SearchCancelledException e) {
        break; // keep best-so-far from previous iteration.
      }
    }

    if (best != null) {
      return best;
    }
    return fallback(state, totalNodes);
  }

  private SearchResult fallback(GameState state, long nodesSoFar) {
    GameStatus status = state.status();
    if (!status.isOngoing()) {
      int score = status.isWin() ? -MinimaxSearch.MATE_SCORE : 0;
      return new SearchResult(null, score, 0, nodesSoFar);
    }
    List<Move> legal = ruleEngine.legalMoves(state);
    if (legal.isEmpty()) {
      return new SearchResult(null, -MinimaxSearch.MATE_SCORE, 0, nodesSoFar);
    }
    return new SearchResult(legal.get(0), 0, 0, nodesSoFar);
  }
}
