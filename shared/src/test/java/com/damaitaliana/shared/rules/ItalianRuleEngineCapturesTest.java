package com.damaitaliana.shared.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.CaptureSequence;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.Square;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItalianRuleEngineCapturesTest {

  private final ItalianRuleEngine engine = new ItalianRuleEngine();

  // --- single captures: men ---

  @Test
  void whiteManCapturesAdjacentBlackManForward() {
    Board b =
        Board.empty()
            .with(new Square(2, 2), white(PieceKind.MAN))
            .with(new Square(3, 3), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s))
        .containsExactly(
            new CaptureSequence(
                new Square(2, 2), List.of(new Square(4, 4)), List.of(new Square(3, 3))));
  }

  @Test
  void blackManCapturesAdjacentWhiteManForwardTowardLowerRank() {
    Board b =
        Board.empty()
            .with(new Square(5, 5), black(PieceKind.MAN))
            .with(new Square(4, 4), white(PieceKind.MAN));
    GameState s = new GameState(b, Color.BLACK, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s))
        .containsExactly(
            new CaptureSequence(
                new Square(5, 5), List.of(new Square(3, 3)), List.of(new Square(4, 4))));
  }

  @Test
  void manCannotCaptureBackward() {
    // White man at (4,4), black man at (3,3) is BEHIND the white man — capture forbidden.
    Board b =
        Board.empty()
            .with(new Square(4, 4), white(PieceKind.MAN))
            .with(new Square(3, 3), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    // No captures available → simple moves apply (one of them is the (4,4)→(5,5) step).
    assertThat(engine.legalMoves(s)).allMatch(m -> !m.isCapture());
  }

  @Test
  void manCannotCaptureFriendly() {
    Board b =
        Board.empty()
            .with(new Square(2, 2), white(PieceKind.MAN))
            .with(new Square(3, 3), white(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s)).allMatch(m -> !m.isCapture());
  }

  @Test
  void captureBlockedByPieceOnLandingSquare() {
    Board b =
        Board.empty()
            .with(new Square(2, 2), white(PieceKind.MAN))
            .with(new Square(3, 3), black(PieceKind.MAN))
            .with(new Square(4, 4), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s)).allMatch(m -> !m.isCapture());
  }

  @Test
  void captureBlockedWhenLandingFallsOffBoard() {
    // White man at (6,6) (dark) jumps over black at (7,7) (dark) → landing (8,8) off board.
    Board b =
        Board.empty()
            .with(new Square(6, 6), white(PieceKind.MAN))
            .with(new Square(7, 7), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s)).allMatch(m -> !m.isCapture());
  }

  // --- single captures: kings ---

  @Test
  void kingCapturesInAllFourDirections() {
    // White king surrounded at (4,4) with one black man on each diagonal at distance 1, all
    // landing squares free.
    Board b =
        Board.empty()
            .with(new Square(4, 4), white(PieceKind.KING))
            .with(new Square(3, 3), black(PieceKind.MAN))
            .with(new Square(5, 5), black(PieceKind.MAN))
            .with(new Square(3, 5), black(PieceKind.MAN))
            .with(new Square(5, 3), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s)).hasSize(4).allMatch(Move::isCapture);
  }

  @Test
  void kingCapturesKing() {
    Board b =
        Board.empty()
            .with(new Square(2, 2), white(PieceKind.KING))
            .with(new Square(3, 3), black(PieceKind.KING));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s))
        .containsExactly(
            new CaptureSequence(
                new Square(2, 2), List.of(new Square(4, 4)), List.of(new Square(3, 3))));
  }

  // --- SPEC §3.3: man cannot capture king ---

  @Test
  void manCannotCaptureKing() {
    Board b =
        Board.empty()
            .with(new Square(2, 2), white(PieceKind.MAN))
            .with(new Square(3, 3), black(PieceKind.KING));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    // No legal capture; the man must use a simple move.
    assertThat(engine.legalMoves(s)).allMatch(m -> !m.isCapture());
  }

  @Test
  void kingCanCaptureMan() {
    Board b =
        Board.empty()
            .with(new Square(2, 2), white(PieceKind.KING))
            .with(new Square(3, 3), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s)).hasSize(1).allMatch(Move::isCapture);
  }

  @Test
  void manCannotCaptureKingButOtherManCanCaptureManIsLegal() {
    // Two white men: one faces a black king (cannot capture), the other faces a black man (can).
    // Mandatory-capture rule: only the legal capture is returned.
    Board b =
        Board.empty()
            .with(new Square(0, 2), white(PieceKind.MAN))
            .with(new Square(1, 3), black(PieceKind.KING)) // forbidden target for white man
            .with(new Square(4, 2), white(PieceKind.MAN))
            .with(new Square(5, 3), black(PieceKind.MAN)); // legal target
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s))
        .containsExactly(
            new CaptureSequence(
                new Square(4, 2), List.of(new Square(6, 4)), List.of(new Square(5, 3))));
  }

  // --- mandatory capture rule (SPEC §3.3) ---

  @Test
  void simpleMovesExcludedWhenAnyCaptureExists() {
    // A white man can either step (3,1)→(4,2) or capture the black man at (4,2).
    Board b =
        Board.empty()
            .with(new Square(3, 1), white(PieceKind.MAN))
            .with(new Square(4, 2), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    List<Move> moves = engine.legalMoves(s);
    assertThat(moves).allMatch(Move::isCapture);
    assertThat(moves)
        .containsExactly(
            new CaptureSequence(
                new Square(3, 1), List.of(new Square(5, 3)), List.of(new Square(4, 2))));
  }

  // --- applyMove on captures ---

  @Test
  void applyMoveRemovesCapturedPieceAndAdvances() {
    Board b =
        Board.empty()
            .with(new Square(2, 2), white(PieceKind.MAN))
            .with(new Square(3, 3), black(PieceKind.MAN));
    GameState before = new GameState(b, Color.WHITE, 7, List.of(), GameStatus.ONGOING);
    CaptureSequence cap =
        new CaptureSequence(new Square(2, 2), List.of(new Square(4, 4)), List.of(new Square(3, 3)));
    GameState after = engine.applyMove(before, cap);
    assertThat(after.board().at(new Square(2, 2))).isEmpty();
    assertThat(after.board().at(new Square(3, 3))).isEmpty();
    assertThat(after.board().at(new Square(4, 4))).contains(white(PieceKind.MAN));
    assertThat(after.sideToMove()).isEqualTo(Color.BLACK);
    assertThat(after.history()).containsExactly(cap);
    // capture resets halfmove clock
    assertThat(after.halfmoveClock()).isZero();
  }

  @Test
  void applyMoveResetsHalfmoveClockOnKingCapture() {
    Board b =
        Board.empty()
            .with(new Square(2, 2), white(PieceKind.KING))
            .with(new Square(3, 3), black(PieceKind.MAN));
    GameState before = new GameState(b, Color.WHITE, 12, List.of(), GameStatus.ONGOING);
    CaptureSequence cap =
        new CaptureSequence(new Square(2, 2), List.of(new Square(4, 4)), List.of(new Square(3, 3)));
    GameState after = engine.applyMove(before, cap);
    assertThat(after.halfmoveClock()).isZero();
  }

  // --- helpers ---

  private static Piece white(PieceKind k) {
    return new Piece(Color.WHITE, k);
  }

  private static Piece black(PieceKind k) {
    return new Piece(Color.BLACK, k);
  }
}
