package com.damaitaliana.shared.ai;

import com.damaitaliana.shared.ai.evaluation.WeightedSumEvaluator;
import com.damaitaliana.shared.ai.search.IterativeDeepeningSearch;
import com.damaitaliana.shared.ai.search.StandardMoveOrderer;
import com.damaitaliana.shared.ai.search.TranspositionTable;
import com.damaitaliana.shared.ai.search.ZobristHasher;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.time.Duration;
import java.util.Objects;

/**
 * Campione (SPEC §12.2): 8 ply, 5000 ms, iterative deepening with a Zobrist-keyed transposition
 * table. The strongest level.
 *
 * <p>The TT and the hasher are owned by this engine for the lifetime of the instance. To free
 * memory between games, drop the engine reference. The TT does <em>not</em> auto-clear; that is
 * intentional, so consecutive {@code chooseMove} calls within the same game reuse work.
 */
public final class CampioneAi implements AiEngine {

  /** Maximum search depth in ply. */
  public static final int MAX_DEPTH = 8;

  /** Default per-move budget; the executor (Task 2.8) translates this to a deadline token. */
  public static final Duration DEFAULT_TIMEOUT = Duration.ofMillis(5000);

  private final TranspositionTable tt;
  private final IterativeDeepeningSearch search;

  /** Uses a fresh {@link ItalianRuleEngine} and a default-sized {@link TranspositionTable}. */
  public CampioneAi() {
    this(new ItalianRuleEngine(), new TranspositionTable(), new ZobristHasher());
  }

  /** Test-friendly constructor that accepts a custom rule engine, TT and hasher. */
  public CampioneAi(RuleEngine ruleEngine, TranspositionTable tt, ZobristHasher hasher) {
    Objects.requireNonNull(ruleEngine, "ruleEngine");
    this.tt = Objects.requireNonNull(tt, "tt");
    Objects.requireNonNull(hasher, "hasher");
    this.search = new IterativeDeepeningSearch(ruleEngine, tt, hasher);
  }

  @Override
  public Move chooseMove(GameState state, CancellationToken cancel) {
    return search
        .search(
            state,
            MAX_DEPTH,
            WeightedSumEvaluator.defaultEvaluator(),
            new StandardMoveOrderer(),
            cancel)
        .bestMove();
  }

  @Override
  public AiLevel level() {
    return AiLevel.CAMPIONE;
  }

  /** Wipes the transposition table. Useful between games. */
  public void clearTranspositionTable() {
    tt.clear();
  }
}
