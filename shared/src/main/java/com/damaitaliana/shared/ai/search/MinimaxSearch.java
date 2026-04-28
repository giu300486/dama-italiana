package com.damaitaliana.shared.ai.search;

import com.damaitaliana.shared.ai.CancellationToken;
import com.damaitaliana.shared.ai.evaluation.Evaluator;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.util.List;
import java.util.Objects;

/**
 * Negamax search with alpha-beta pruning (SPEC §12.1).
 *
 * <p>Returns the principal-variation move at the root and its score in centipawns from the root
 * sideToMove's perspective. Terminal positions (a side has won, or the position is a draw) are
 * scored {@code ±({@value #MATE_SCORE} − plyFromRoot)} so that the search distinguishes mate-in-1
 * from mate-in-N (a closer mate is better for the winner; a closer loss is worse for the loser).
 *
 * <p>This task ships the fixed-depth variant used by the Esperto level (Task 2.7). Iterative
 * deepening (Task 2.5) and the transposition table (Task 2.6) layer on top of this primitive
 * without changing its contract.
 */
public final class MinimaxSearch {

  /** Score returned for a forced win. Larger than any possible heuristic evaluation. */
  public static final int MATE_SCORE = 1_000_000;

  /** Initial alpha/beta sentinel: strictly larger than {@link #MATE_SCORE}. */
  public static final int INFINITY = MATE_SCORE + 1;

  private final RuleEngine ruleEngine;

  /** Uses a fresh {@link ItalianRuleEngine}. */
  public MinimaxSearch() {
    this(new ItalianRuleEngine());
  }

  /** Uses an externally provided rule engine — useful for tests with mocks/spies. */
  public MinimaxSearch(RuleEngine ruleEngine) {
    this.ruleEngine = Objects.requireNonNull(ruleEngine, "ruleEngine");
  }

  /**
   * Runs a fixed-depth negamax search of {@code state}.
   *
   * @param state the position to search from. Must be {@link GameStatus#ONGOING}; if it is already
   *     terminal the result has a {@code null} bestMove and the appropriate terminal score.
   * @param depth the search depth in ply. Must be {@code >= 1}.
   * @param evaluator the static evaluator used at depth-zero leaves.
   * @param orderer the move orderer applied at every node before iterating children.
   * @param cancel cooperative cancellation; checked at every node entry.
   * @return search outcome with bestMove, score, depth reached and nodes visited.
   * @throws com.damaitaliana.shared.ai.SearchCancelledException if {@code cancel} is cancelled
   *     during the search.
   */
  public SearchResult search(
      GameState state,
      int depth,
      Evaluator evaluator,
      MoveOrderer orderer,
      CancellationToken cancel) {
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(evaluator, "evaluator");
    Objects.requireNonNull(orderer, "orderer");
    Objects.requireNonNull(cancel, "cancel");
    if (depth < 1) {
      throw new IllegalArgumentException("depth must be >= 1: " + depth);
    }

    Stats stats = new Stats();
    cancel.throwIfCancelled();
    stats.nodes++;

    GameStatus status = state.status();
    if (!status.isOngoing()) {
      return new SearchResult(null, terminalScore(status, 0), depth, stats.nodes);
    }

    List<Move> legal = ruleEngine.legalMoves(state);
    if (legal.isEmpty()) {
      return new SearchResult(null, -(MATE_SCORE - 0), depth, stats.nodes);
    }

    List<Move> ordered = orderer.order(legal, state);
    Move bestMove = ordered.get(0);
    int bestScore = -INFINITY;
    int alpha = -INFINITY;
    int beta = INFINITY;

    for (Move move : ordered) {
      GameState next = ruleEngine.applyMove(state, move);
      int score = -negamax(next, depth - 1, 1, -beta, -alpha, evaluator, orderer, cancel, stats);
      if (score > bestScore) {
        bestScore = score;
        bestMove = move;
      }
      if (bestScore > alpha) {
        alpha = bestScore;
      }
      if (alpha >= beta) {
        break;
      }
    }

    return new SearchResult(bestMove, bestScore, depth, stats.nodes);
  }

  private int negamax(
      GameState state,
      int depthRemaining,
      int plyFromRoot,
      int alpha,
      int beta,
      Evaluator evaluator,
      MoveOrderer orderer,
      CancellationToken cancel,
      Stats stats) {
    cancel.throwIfCancelled();
    stats.nodes++;

    GameStatus status = state.status();
    if (!status.isOngoing()) {
      return terminalScore(status, plyFromRoot);
    }
    if (depthRemaining == 0) {
      return evaluator.evaluate(state, state.sideToMove());
    }

    List<Move> legal = ruleEngine.legalMoves(state);
    if (legal.isEmpty()) {
      return -(MATE_SCORE - plyFromRoot);
    }

    List<Move> ordered = orderer.order(legal, state);
    int best = -INFINITY;

    for (Move move : ordered) {
      GameState next = ruleEngine.applyMove(state, move);
      int score =
          -negamax(
              next,
              depthRemaining - 1,
              plyFromRoot + 1,
              -beta,
              -alpha,
              evaluator,
              orderer,
              cancel,
              stats);
      if (score > best) {
        best = score;
      }
      if (best > alpha) {
        alpha = best;
      }
      if (alpha >= beta) {
        break;
      }
    }

    return best;
  }

  /** A draw is 0; a terminal win/loss is mate distance penalised. */
  private static int terminalScore(GameStatus status, int plyFromRoot) {
    if (status.isDraw()) {
      return 0;
    }
    // status.isWin(): the side that just moved won, so the current sideToMove has lost.
    return -(MATE_SCORE - plyFromRoot);
  }

  private static final class Stats {
    long nodes;
  }
}
