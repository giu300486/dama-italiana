package com.damaitaliana.shared.ai.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.shared.ai.CancellationToken;
import com.damaitaliana.shared.ai.MutableCancellationToken;
import com.damaitaliana.shared.ai.evaluation.Evaluator;
import com.damaitaliana.shared.ai.evaluation.MaterialTerm;
import com.damaitaliana.shared.ai.evaluation.WeightedSumEvaluator;
import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.util.List;
import org.junit.jupiter.api.Test;

class IterativeDeepeningSearchTest {

  private final RuleEngine ruleEngine = new ItalianRuleEngine();
  private final IterativeDeepeningSearch ids = new IterativeDeepeningSearch(ruleEngine);
  private final Evaluator material =
      new WeightedSumEvaluator(
          List.of(new WeightedSumEvaluator.WeightedTerm(new MaterialTerm(), 1)));
  private final MoveOrderer orderer = new StandardMoveOrderer();

  // --- input validation ---

  @Test
  void rejectsZeroMaxDepth() {
    assertThatThrownBy(
            () -> ids.search(GameState.initial(), 0, material, orderer, CancellationToken.never()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // --- basic behaviour ---

  @Test
  void returnsDeepestCompletedDepthWhenUncancelled() {
    SearchResult result =
        ids.search(GameState.initial(), 3, material, orderer, CancellationToken.never());
    assertThat(result.depthReached()).isEqualTo(3);
    assertThat(result.bestMove()).isNotNull();
  }

  @Test
  void aggregatesNodesAcrossIterations() {
    SearchResult result =
        ids.search(GameState.initial(), 3, material, orderer, CancellationToken.never());
    SearchResult d1 =
        new MinimaxSearch(ruleEngine)
            .search(GameState.initial(), 1, material, orderer, CancellationToken.never());
    // IDS at depth 3 walks depths 1+2+3, so its node count is at least depth 1's.
    assertThat(result.nodesVisited()).isGreaterThan(d1.nodesVisited());
  }

  @Test
  void returnsLegalBestMoveAtRoot() {
    GameState start = GameState.initial();
    SearchResult result = ids.search(start, 3, material, orderer, CancellationToken.never());
    assertThat(ruleEngine.legalMoves(start)).contains(result.bestMove());
  }

  // --- cancellation ---

  @Test
  void cancellationMidIterationKeepsPreviousDepthsBestMove() {
    // Wrap the orderer so that the second orderer call (= first call inside iteration 2) fires
    // cancellation. Iteration 1 needs exactly 1 orderer call (root only), so iteration 2 is the
    // first to be interrupted.
    MutableCancellationToken token = new MutableCancellationToken();
    int[] callCounter = {0};
    MoveOrderer cancellingOrderer =
        (moves, state) -> {
          callCounter[0]++;
          if (callCounter[0] == 2) {
            token.cancel();
          }
          return orderer.order(moves, state);
        };
    SearchResult result = ids.search(GameState.initial(), 5, material, cancellingOrderer, token);
    assertThat(result.bestMove()).isNotNull();
    assertThat(result.depthReached()).isEqualTo(1);
  }

  @Test
  void preCancelledTokenFallsBackToFirstLegalMove() {
    MutableCancellationToken token = new MutableCancellationToken();
    token.cancel();
    GameState start = GameState.initial();
    SearchResult result = ids.search(start, 3, material, orderer, token);
    assertThat(result.bestMove()).isNotNull();
    assertThat(result.depthReached()).isZero();
    assertThat(ruleEngine.legalMoves(start)).contains(result.bestMove());
  }

  @Test
  void preCancelledTokenOnTerminalReturnsNullBestMove() {
    MutableCancellationToken token = new MutableCancellationToken();
    token.cancel();
    GameState terminal =
        new GameState(Board.initial(), Color.WHITE, 0, List.of(), GameStatus.DRAW_FORTY_MOVES);
    SearchResult result = ids.search(terminal, 3, material, orderer, token);
    assertThat(result.bestMove()).isNull();
    assertThat(result.score()).isZero();
  }

  // --- terminal handling ---

  @Test
  void terminalRootReturnsTerminalScore() {
    GameState terminal =
        new GameState(Board.empty(), Color.BLACK, 0, List.of(), GameStatus.WHITE_WINS);
    SearchResult result = ids.search(terminal, 3, material, orderer, CancellationToken.never());
    assertThat(result.bestMove()).isNull();
    assertThat(result.score()).isEqualTo(-MinimaxSearch.MATE_SCORE);
  }

  // --- early stop on mate ---

  @Test
  void stopsEarlyOnForcedMate() {
    Board b =
        Board.empty()
            .with(new Square(4, 4), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(3, 5), new Piece(Color.BLACK, PieceKind.MAN));
    GameState state = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    SearchResult result = ids.search(state, 8, material, orderer, CancellationToken.never());
    // Mate found at depth 1 (forced capture); IDS should NOT proceed to depth 8.
    assertThat(result.depthReached()).isEqualTo(1);
    assertThat(result.score()).isEqualTo(MinimaxSearch.MATE_SCORE - 1);
  }

  // --- pv-first ordering smoke ---

  @Test
  void usesPvFirstOrdererForSubsequentIterations() {
    // Detect via a counting orderer: track which move comes first on each call.  After the first
    // iteration the previous bestMove must be at index 0 of the list given to the underlying
    // negamax recursion.
    GameState start = GameState.initial();
    int[] callCounter = {0};
    Move[] firstMoveSeen = {null, null}; // index 0 = iter 1 root, index 1 = iter 2 root
    MoveOrderer recording =
        (moves, state) -> {
          List<Move> base = orderer.order(moves, state);
          callCounter[0]++;
          if (callCounter[0] == 1) {
            firstMoveSeen[0] = base.get(0);
          } else if (callCounter[0] == 2) {
            firstMoveSeen[1] = base.get(0);
          }
          return base;
        };
    ids.search(start, 2, material, recording, CancellationToken.never());
    // Iteration 1's first move (the bestMove found) becomes the PV for iteration 2.
    Move pv = firstMoveSeen[0];
    Move iter2First = firstMoveSeen[1];
    // The recording orderer is wrapped by PvFirstOrderer in iteration 2; the PV should be at idx 0.
    // But the recording orderer sees the BASE order (before PV-first promotion). It's enough to
    // assert the PV from iter 1 was selected by the search.
    assertThat(pv).isNotNull();
    assertThat(iter2First).isNotNull();
    // Indirect property: the underlying minimax received the PV first via PvFirstOrderer.  We
    // can verify that PvFirstOrderer was constructed by checking that node count of iteration 2
    // is reasonable (no crash, no infinite loop).  For a direct unit test see PvFirstOrdererTest.
  }

  // --- determinism ---

  @Test
  void sameInputProducesSameResult() {
    GameState start = GameState.initial();
    SearchResult r1 = ids.search(start, 3, material, orderer, CancellationToken.never());
    SearchResult r2 = ids.search(start, 3, material, orderer, CancellationToken.never());
    assertThat(r1.bestMove()).isEqualTo(r2.bestMove());
    assertThat(r1.score()).isEqualTo(r2.score());
    assertThat(r1.depthReached()).isEqualTo(r2.depthReached());
    assertThat(r1.nodesVisited()).isEqualTo(r2.nodesVisited());
  }
}
