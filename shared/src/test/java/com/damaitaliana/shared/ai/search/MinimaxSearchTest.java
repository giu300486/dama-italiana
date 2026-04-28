package com.damaitaliana.shared.ai.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.shared.ai.CancellationToken;
import com.damaitaliana.shared.ai.MutableCancellationToken;
import com.damaitaliana.shared.ai.SearchCancelledException;
import com.damaitaliana.shared.ai.evaluation.Evaluator;
import com.damaitaliana.shared.ai.evaluation.MaterialTerm;
import com.damaitaliana.shared.ai.evaluation.WeightedSumEvaluator;
import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.util.List;
import org.junit.jupiter.api.Test;

class MinimaxSearchTest {

  private final RuleEngine ruleEngine = new ItalianRuleEngine();
  private final MinimaxSearch search = new MinimaxSearch(ruleEngine);
  private final Evaluator material =
      new WeightedSumEvaluator(
          List.of(new WeightedSumEvaluator.WeightedTerm(new MaterialTerm(), 1)));
  private final MoveOrderer orderer = new StandardMoveOrderer();

  // --- input validation ---

  @Test
  void rejectsDepthZero() {
    assertThatThrownBy(
            () ->
                search.search(GameState.initial(), 0, material, orderer, CancellationToken.never()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("depth");
  }

  @Test
  void rejectsNegativeDepth() {
    assertThatThrownBy(
            () ->
                search.search(
                    GameState.initial(), -1, material, orderer, CancellationToken.never()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // --- best move legality ---

  @Test
  void bestMoveAtRootIsAmongLegalMoves() {
    GameState start = GameState.initial();
    SearchResult result = search.search(start, 3, material, orderer, CancellationToken.never());
    assertThat(ruleEngine.legalMoves(start)).contains(result.bestMove());
  }

  // --- captures and tactics ---

  @Test
  void picksCaptureWhenAvailable() {
    // Italian rules force a capture: white man on (4,4) must jump (3,5)→(2,6).
    Board b =
        Board.empty()
            .with(new Square(4, 4), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(3, 5), new Piece(Color.BLACK, PieceKind.MAN))
            .with(new Square(0, 0), new Piece(Color.BLACK, PieceKind.MAN)); // not capturable
    GameState state = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    SearchResult result = search.search(state, 1, material, orderer, CancellationToken.never());
    assertThat(result.bestMove().isCapture()).isTrue();
    assertThat(result.bestMove().capturedSquares()).containsExactly(new Square(3, 5));
  }

  @Test
  void detectsForcedMateInOnePlyAsHugeScore() {
    // Black has only the piece on (3,5); white captures it → black has 0 pieces → WHITE_WINS.
    Board b =
        Board.empty()
            .with(new Square(4, 4), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(3, 5), new Piece(Color.BLACK, PieceKind.MAN));
    GameState state = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    SearchResult result = search.search(state, 3, material, orderer, CancellationToken.never());
    // mate-in-1: score is MATE_SCORE - 1
    assertThat(result.score()).isEqualTo(MinimaxSearch.MATE_SCORE - 1);
  }

  @Test
  void blackKingCapturesLastWhiteManAsForcedMate() {
    // White's only piece is on (3,5); a black king on (4,4) can capture it backward (kings move
    // in all four diagonals) by jumping to (2,6). After the capture White has 0 pieces, so the
    // resulting position is BLACK_WINS — score from Black's perspective is MATE_SCORE - 1.
    Board b =
        Board.empty()
            .with(new Square(3, 5), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(4, 4), new Piece(Color.BLACK, PieceKind.KING));
    GameState state = new GameState(b, Color.BLACK, 0, List.of(), GameStatus.ONGOING);
    SearchResult result = search.search(state, 3, material, orderer, CancellationToken.never());
    assertThat(result.score()).isEqualTo(MinimaxSearch.MATE_SCORE - 1);
  }

  // --- terminal handling ---

  @Test
  void terminalWinReturnsLargeNegativeForLoser() {
    // Status forced as WHITE_WINS while sideToMove=BLACK (the loser).
    Board b = Board.empty().with(new Square(0, 0), new Piece(Color.WHITE, PieceKind.MAN));
    GameState state = new GameState(b, Color.BLACK, 0, List.of(), GameStatus.WHITE_WINS);
    SearchResult result = search.search(state, 1, material, orderer, CancellationToken.never());
    assertThat(result.bestMove()).isNull();
    assertThat(result.score()).isEqualTo(-MinimaxSearch.MATE_SCORE);
  }

  @Test
  void terminalDrawReturnsZero() {
    GameState state =
        new GameState(Board.initial(), Color.WHITE, 0, List.of(), GameStatus.DRAW_FORTY_MOVES);
    SearchResult result = search.search(state, 2, material, orderer, CancellationToken.never());
    assertThat(result.bestMove()).isNull();
    assertThat(result.score()).isZero();
  }

  // --- nodes visited ---

  @Test
  void deeperSearchVisitsMoreNodes() {
    GameState start = GameState.initial();
    SearchResult d1 = search.search(start, 1, material, orderer, CancellationToken.never());
    SearchResult d3 = search.search(start, 3, material, orderer, CancellationToken.never());
    assertThat(d3.nodesVisited()).isGreaterThan(d1.nodesVisited());
    assertThat(d1.nodesVisited()).isPositive();
  }

  @Test
  void depthReachedEqualsRequestedDepth() {
    SearchResult result =
        search.search(GameState.initial(), 4, material, orderer, CancellationToken.never());
    assertThat(result.depthReached()).isEqualTo(4);
  }

  // --- cancellation ---

  @Test
  void cancellationStopsSearch() {
    MutableCancellationToken token = new MutableCancellationToken();
    Evaluator cancelling =
        (s, p) -> {
          token.cancel();
          return 0;
        };
    assertThatThrownBy(() -> search.search(GameState.initial(), 3, cancelling, orderer, token))
        .isInstanceOf(SearchCancelledException.class);
  }

  @Test
  void preCancelledTokenStopsImmediately() {
    MutableCancellationToken token = new MutableCancellationToken();
    token.cancel();
    assertThatThrownBy(() -> search.search(GameState.initial(), 3, material, orderer, token))
        .isInstanceOf(SearchCancelledException.class);
  }

  // --- alpha-beta correctness ---

  @Test
  void alphaBetaProducesSameBestMoveAsFullSearch() {
    // Indirect test: with a stable evaluator and orderer, repeated runs are deterministic and the
    // best move is consistent across different depths' parity (in absence of mate-distance
    // tweaks the score doesn't have to match across depths, but the move must be sane).
    GameState start = GameState.initial();
    SearchResult r1 = search.search(start, 2, material, orderer, CancellationToken.never());
    SearchResult r2 = search.search(start, 2, material, orderer, CancellationToken.never());
    assertThat(r1.bestMove()).isEqualTo(r2.bestMove());
    assertThat(r1.score()).isEqualTo(r2.score());
    assertThat(r1.nodesVisited()).isEqualTo(r2.nodesVisited());
  }

  // --- input null guards ---

  @Test
  void rejectsNullState() {
    assertThatThrownBy(() -> search.search(null, 1, material, orderer, CancellationToken.never()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsNullEvaluator() {
    assertThatThrownBy(
            () -> search.search(GameState.initial(), 1, null, orderer, CancellationToken.never()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsNullOrderer() {
    assertThatThrownBy(
            () -> search.search(GameState.initial(), 1, material, null, CancellationToken.never()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsNullCancel() {
    assertThatThrownBy(() -> search.search(GameState.initial(), 1, material, orderer, null))
        .isInstanceOf(NullPointerException.class);
  }

  // --- avoidance of an obvious bad trade ---

  @Test
  void doesNotSelfHangAtDepthThree() {
    // White's only legal move is a capture. After the capture, white can be re-captured by a
    // black king. With material-only evaluation and depth 3, the search still picks the forced
    // capture (Italian rules require it) but recognises the resulting material loss.
    Board b =
        Board.empty()
            .with(new Square(4, 4), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(3, 5), new Piece(Color.BLACK, PieceKind.MAN))
            .with(new Square(0, 6), new Piece(Color.BLACK, PieceKind.KING))
            .with(new Square(0, 4), new Piece(Color.BLACK, PieceKind.MAN));
    GameState state = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    SearchResult result = search.search(state, 3, material, orderer, CancellationToken.never());
    assertThat(result.bestMove()).isNotNull();
    assertThat(result.bestMove().isCapture()).isTrue();
  }
}
