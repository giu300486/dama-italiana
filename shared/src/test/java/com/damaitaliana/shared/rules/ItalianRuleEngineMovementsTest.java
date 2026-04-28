package com.damaitaliana.shared.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.CaptureSequence;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.SimpleMove;
import com.damaitaliana.shared.domain.Square;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItalianRuleEngineMovementsTest {

  private final ItalianRuleEngine engine = new ItalianRuleEngine();

  // --- legalMoves: men ---

  @Test
  void whiteManInTheMiddleHasTwoForwardSimpleMoves() {
    Board b = Board.empty().with(new Square(4, 4), white(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s))
        .containsExactlyInAnyOrder(
            new SimpleMove(new Square(4, 4), new Square(3, 5)),
            new SimpleMove(new Square(4, 4), new Square(5, 5)));
  }

  @Test
  void blackManInTheMiddleHasTwoForwardSimpleMovesTowardLowerRank() {
    Board b = Board.empty().with(new Square(4, 4), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.BLACK, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s))
        .containsExactlyInAnyOrder(
            new SimpleMove(new Square(4, 4), new Square(3, 3)),
            new SimpleMove(new Square(4, 4), new Square(5, 3)));
  }

  @Test
  void manNeverMovesBackward() {
    Board b = Board.empty().with(new Square(4, 4), white(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    List<Move> moves = engine.legalMoves(s);
    assertThat(moves)
        .doesNotContain(new SimpleMove(new Square(4, 4), new Square(3, 3)))
        .doesNotContain(new SimpleMove(new Square(4, 4), new Square(5, 3)));
  }

  @Test
  void manAtFileZeroOnlyHasOneForwardMove() {
    Board b = Board.empty().with(new Square(0, 4), white(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s))
        .containsExactly(new SimpleMove(new Square(0, 4), new Square(1, 5)));
  }

  @Test
  void manAtPromotionRowHasNoMoves() {
    // A man already on the last opponent row cannot move further forward; in normal play it
    // would have been promoted, but here we simulate the corner case.
    Board b = Board.empty().with(new Square(3, 7), white(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s)).isEmpty();
  }

  @Test
  void manBlockedByFriendlyPieceHasNoMove() {
    Board b =
        Board.empty()
            .with(new Square(4, 4), white(PieceKind.MAN))
            .with(new Square(3, 5), white(PieceKind.MAN))
            .with(new Square(5, 5), white(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    // The two front pieces still have moves; the back man at (4,4) is blocked.
    assertThat(engine.legalMoves(s)).extracting(Move::from).doesNotContain(new Square(4, 4));
  }

  // --- legalMoves: kings ---

  @Test
  void kingInTheMiddleHasFourSimpleMoves() {
    Board b = Board.empty().with(new Square(4, 4), white(PieceKind.KING));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s)).hasSize(4);
  }

  @Test
  void kingMovesOnlyOneStepNotFlying() {
    Board b = Board.empty().with(new Square(4, 4), white(PieceKind.KING));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s))
        .extracting(m -> m.to())
        .doesNotContain(new Square(2, 2), new Square(6, 6));
  }

  @Test
  void kingAtCornerHasOneMove() {
    Board b = Board.empty().with(new Square(0, 0), white(PieceKind.KING));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s))
        .containsExactly(new SimpleMove(new Square(0, 0), new Square(1, 1)));
  }

  // --- side to move filtering ---

  @Test
  void onlyPiecesOfSideToMoveAreConsidered() {
    Board b =
        Board.empty()
            .with(new Square(4, 4), white(PieceKind.MAN))
            .with(new Square(2, 4), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s)).extracting(Move::from).containsOnly(new Square(4, 4));
  }

  @Test
  void initialPositionWhiteHasSevenSimpleMoves() {
    // From the standard starting position White has 7 simple moves: every man on rank 2 with at
    // least one empty diagonal forward target lying on rank 3.
    GameState s = GameState.initial();
    List<Move> moves = engine.legalMoves(s);
    assertThat(moves).hasSize(7).allMatch(m -> !m.isCapture());
    assertThat(moves).extracting(Move::from).allMatch(sq -> sq.rank() == 2);
  }

  // --- legalMoves: edge cases ---

  @Test
  void noMovesWhenStateIsNotOngoing() {
    GameState s = new GameState(Board.initial(), Color.WHITE, 0, List.of(), GameStatus.WHITE_WINS);
    assertThat(engine.legalMoves(s)).isEmpty();
  }

  @Test
  void noMovesOnEmptyBoard() {
    GameState s = new GameState(Board.empty(), Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s)).isEmpty();
  }

  @Test
  void legalMovesRejectsNullState() {
    assertThatThrownBy(() -> engine.legalMoves(null)).isInstanceOf(NullPointerException.class);
  }

  // --- applyMove (simple movements only) ---

  @Test
  void applyMoveAdvancesPieceAndTogglesSide() {
    Board b = Board.empty().with(new Square(4, 4), white(PieceKind.MAN));
    GameState before = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    SimpleMove move = new SimpleMove(new Square(4, 4), new Square(5, 5));
    GameState after = engine.applyMove(before, move);
    assertThat(after.board().at(new Square(4, 4))).isEmpty();
    assertThat(after.board().at(new Square(5, 5))).contains(white(PieceKind.MAN));
    assertThat(after.sideToMove()).isEqualTo(Color.BLACK);
    assertThat(after.history()).containsExactly(move);
  }

  @Test
  void applyMoveResetsHalfmoveClockOnManMove() {
    Board b = Board.empty().with(new Square(4, 4), white(PieceKind.MAN));
    GameState before = new GameState(b, Color.WHITE, 7, List.of(), GameStatus.ONGOING);
    SimpleMove move = new SimpleMove(new Square(4, 4), new Square(5, 5));
    GameState after = engine.applyMove(before, move);
    assertThat(after.halfmoveClock()).isZero();
  }

  @Test
  void applyMoveIncrementsHalfmoveClockOnKingSimpleMove() {
    Board b = Board.empty().with(new Square(4, 4), white(PieceKind.KING));
    GameState before = new GameState(b, Color.WHITE, 5, List.of(), GameStatus.ONGOING);
    SimpleMove move = new SimpleMove(new Square(4, 4), new Square(5, 5));
    GameState after = engine.applyMove(before, move);
    assertThat(after.halfmoveClock()).isEqualTo(6);
  }

  @Test
  void applyMoveRejectsIllegalMove() {
    Board b = Board.empty().with(new Square(4, 4), white(PieceKind.MAN));
    GameState before = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    Move backward = new SimpleMove(new Square(4, 4), new Square(3, 3));
    assertThatThrownBy(() -> engine.applyMove(before, backward))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("not legal");
  }

  @Test
  void applyMoveRejectsCaptureSequenceUntilTask14() {
    Board b = Board.empty().with(new Square(4, 4), white(PieceKind.MAN));
    GameState before = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    Move cap =
        new CaptureSequence(new Square(4, 4), List.of(new Square(2, 2)), List.of(new Square(3, 3)));
    assertThatThrownBy(() -> engine.applyMove(before, cap))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("capture sequences not yet supported");
  }

  @Test
  void applyMoveRejectsNullArguments() {
    GameState s = GameState.initial();
    assertThatThrownBy(
            () -> engine.applyMove(null, new SimpleMove(new Square(0, 0), new Square(1, 1))))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> engine.applyMove(s, null)).isInstanceOf(NullPointerException.class);
  }

  // --- computeStatus (stub: ongoing only) ---

  @Test
  void computeStatusReturnsOngoingForNow() {
    assertThat(engine.computeStatus(GameState.initial())).isEqualTo(GameStatus.ONGOING);
  }

  @Test
  void computeStatusRejectsNull() {
    assertThatThrownBy(() -> engine.computeStatus(null)).isInstanceOf(NullPointerException.class);
  }

  // --- helpers ---

  private static Piece white(PieceKind k) {
    return new Piece(Color.WHITE, k);
  }

  private static Piece black(PieceKind k) {
    return new Piece(Color.BLACK, k);
  }
}
