package com.damaitaliana.shared.ai.search;

import com.damaitaliana.shared.ai.CancellationToken;
import com.damaitaliana.shared.ai.evaluation.Evaluator;
import com.damaitaliana.shared.ai.search.TranspositionTable.NodeType;
import com.damaitaliana.shared.ai.search.TranspositionTable.TtEntry;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.util.ArrayList;
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
 * <p>Two flavours, selected at construction:
 *
 * <ul>
 *   <li>Plain alpha-beta — pass only the {@link RuleEngine} (or use the no-arg constructor).
 *   <li>TT-enabled — pass a {@link TranspositionTable} and a {@link ZobristHasher}. The search
 *       probes the TT before expanding a node and stores the result on exit, dramatically cutting
 *       repeated work in the iterative-deepening loop driving the Campione level.
 * </ul>
 */
public final class MinimaxSearch {

  /** Score returned for a forced win. Larger than any possible heuristic evaluation. */
  public static final int MATE_SCORE = 1_000_000;

  /** Initial alpha/beta sentinel: strictly larger than {@link #MATE_SCORE}. */
  public static final int INFINITY = MATE_SCORE + 1;

  private final RuleEngine ruleEngine;
  private final TranspositionTable tt;
  private final ZobristHasher hasher;

  /** Uses a fresh {@link ItalianRuleEngine}; no transposition table. */
  public MinimaxSearch() {
    this(new ItalianRuleEngine(), null, null);
  }

  /** Uses an externally provided rule engine; no transposition table. */
  public MinimaxSearch(RuleEngine ruleEngine) {
    this(ruleEngine, null, null);
  }

  /**
   * TT-enabled constructor. Both {@code tt} and {@code hasher} must be non-{@code null}; they are
   * always used together.
   */
  public MinimaxSearch(RuleEngine ruleEngine, TranspositionTable tt, ZobristHasher hasher) {
    this.ruleEngine = Objects.requireNonNull(ruleEngine, "ruleEngine");
    if ((tt == null) != (hasher == null)) {
      throw new IllegalArgumentException("tt and hasher must be both null or both non-null");
    }
    this.tt = tt;
    this.hasher = hasher;
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
    int score = negamax(state, depth, 0, -INFINITY, INFINITY, evaluator, orderer, cancel, stats);
    return new SearchResult(stats.rootBestMove, score, depth, stats.nodes);
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

    int alphaOrig = alpha;
    long hashKey = 0L;
    Move ttBestMove = null;
    if (tt != null) {
      hashKey = hasher.hash(state);
      TtEntry entry = tt.probe(hashKey);
      if (entry != null) {
        ttBestMove = entry.bestMove();
        // At root we never early-return from the TT: the caller relies on stats.rootBestMove,
        // which is only set after the move loop has produced one. Inner nodes return freely.
        if (plyFromRoot > 0 && entry.depth() >= depthRemaining) {
          int s = entry.score();
          switch (entry.type()) {
            case EXACT:
              return s;
            case LOWER_BOUND:
              if (s >= beta) {
                return s;
              }
              alpha = Math.max(alpha, s);
              break;
            case UPPER_BOUND:
              if (s <= alpha) {
                return s;
              }
              beta = Math.min(beta, s);
              break;
            default:
              throw new AssertionError(entry.type());
          }
          if (alpha >= beta) {
            return s;
          }
        }
      }
    }

    List<Move> legal = ruleEngine.legalMoves(state);
    if (legal.isEmpty()) {
      return -(MATE_SCORE - plyFromRoot);
    }

    List<Move> ordered = orderWithTtFirst(orderer.order(legal, state), ttBestMove);
    int best = -INFINITY;
    Move bestMove = ordered.get(0);

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
        bestMove = move;
      }
      if (best > alpha) {
        alpha = best;
      }
      if (alpha >= beta) {
        break;
      }
    }

    if (plyFromRoot == 0) {
      stats.rootBestMove = bestMove;
    }

    if (tt != null) {
      NodeType type;
      if (best <= alphaOrig) {
        type = NodeType.UPPER_BOUND;
      } else if (best >= beta) {
        type = NodeType.LOWER_BOUND;
      } else {
        type = NodeType.EXACT;
      }
      tt.store(new TtEntry(hashKey, best, depthRemaining, type, bestMove));
    }

    return best;
  }

  private static List<Move> orderWithTtFirst(List<Move> base, Move ttMove) {
    if (ttMove == null || !base.contains(ttMove) || base.get(0).equals(ttMove)) {
      return base;
    }
    List<Move> result = new ArrayList<>(base.size());
    result.add(ttMove);
    for (Move m : base) {
      if (!m.equals(ttMove)) {
        result.add(m);
      }
    }
    return List.copyOf(result);
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
    Move rootBestMove;
  }
}
