package com.damaitaliana.shared.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.SimpleMove;
import com.damaitaliana.shared.domain.Square;
import java.util.List;
import org.junit.jupiter.api.Test;

/** SPEC §3.6 — game status: win, stalemate-loss, 40-move draw, threefold repetition. */
class ItalianRuleEngineStatusTest {

  private final ItalianRuleEngine engine = new ItalianRuleEngine();

  // --- ongoing ---

  @Test
  void initialPositionIsOngoing() {
    assertThat(engine.computeStatus(GameState.initial())).isEqualTo(GameStatus.ONGOING);
  }

  // --- no pieces left ---

  @Test
  void blackWinsWhenWhiteHasNoPieces() {
    Board b = Board.empty().with(new Square(4, 4), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.computeStatus(s)).isEqualTo(GameStatus.BLACK_WINS);
  }

  @Test
  void whiteWinsWhenBlackHasNoPieces() {
    Board b = Board.empty().with(new Square(4, 4), white(PieceKind.MAN));
    GameState s = new GameState(b, Color.BLACK, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.computeStatus(s)).isEqualTo(GameStatus.WHITE_WINS);
  }

  // --- stalemate (Italian variant: stalemate is a LOSS for the side to move) ---

  @Test
  void blackWinsWhenWhiteIsStalemated() {
    // White king cornered with no possible move and no possible capture.
    // (7,7) — only diag step is (6,6); (6,6) has a black king blocking. Capture would land on
    // (5,5) which is also occupied by black → no jump.
    Board b =
        Board.empty()
            .with(new Square(7, 7), white(PieceKind.KING))
            .with(new Square(6, 6), black(PieceKind.KING))
            .with(new Square(5, 5), black(PieceKind.KING));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.computeStatus(s)).isEqualTo(GameStatus.BLACK_WINS);
  }

  @Test
  void whiteWinsWhenBlackIsStalemated() {
    Board b =
        Board.empty()
            .with(new Square(0, 0), black(PieceKind.KING))
            .with(new Square(1, 1), white(PieceKind.KING))
            .with(new Square(2, 2), white(PieceKind.KING));
    GameState s = new GameState(b, Color.BLACK, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.computeStatus(s)).isEqualTo(GameStatus.WHITE_WINS);
  }

  // --- 40-move rule ---

  @Test
  void drawByFortyMovesWhenHalfmoveClockReaches80() {
    Board b =
        Board.empty()
            .with(new Square(2, 4), white(PieceKind.KING))
            .with(new Square(0, 4), black(PieceKind.KING));
    GameState s = new GameState(b, Color.WHITE, 80, List.of(), GameStatus.ONGOING);
    assertThat(engine.computeStatus(s)).isEqualTo(GameStatus.DRAW_FORTY_MOVES);
  }

  @Test
  void noFortyMovesDrawAtClock79() {
    Board b =
        Board.empty()
            .with(new Square(2, 4), white(PieceKind.KING))
            .with(new Square(0, 4), black(PieceKind.KING));
    GameState s = new GameState(b, Color.WHITE, 79, List.of(), GameStatus.ONGOING);
    assertThat(engine.computeStatus(s)).isEqualTo(GameStatus.ONGOING);
  }

  // --- precedence: a win takes priority over a draw ---

  @Test
  void noPiecesTakesPrecedenceOverFortyMoves() {
    Board b = Board.empty().with(new Square(4, 4), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 80, List.of(), GameStatus.ONGOING);
    assertThat(engine.computeStatus(s)).isEqualTo(GameStatus.BLACK_WINS);
  }

  @Test
  void stalemateTakesPrecedenceOverFortyMoves() {
    Board b =
        Board.empty()
            .with(new Square(7, 7), white(PieceKind.KING))
            .with(new Square(6, 6), black(PieceKind.KING))
            .with(new Square(5, 5), black(PieceKind.KING));
    GameState s = new GameState(b, Color.WHITE, 80, List.of(), GameStatus.ONGOING);
    assertThat(engine.computeStatus(s)).isEqualTo(GameStatus.BLACK_WINS);
  }

  // --- threefold repetition: short-history corner ---

  @Test
  void historyShorterThanFourPliesIsNeverRepetition() {
    Board b =
        Board.empty()
            .with(new Square(2, 4), white(PieceKind.KING))
            .with(new Square(0, 4), black(PieceKind.KING));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.computeStatus(s)).isEqualTo(GameStatus.ONGOING);
  }

  @Test
  void replayingAValidGameDoesNotCrashRepetitionDetection() {
    // The repetition check replays the move history from GameState.initial(). This test makes
    // sure a short, real sequence of legal moves is handled without exception and yields
    // ONGOING (no position has recurred three times yet). Threefold repetition itself is
    // exercised by the corpus and end-to-end tests. Each move is chosen so neither side gains a
    // mandatory capture immediately afterwards.
    GameState s = GameState.initial();
    s = engine.applyMove(s, new SimpleMove(new Square(0, 2), new Square(1, 3))); // W
    s = engine.applyMove(s, new SimpleMove(new Square(7, 5), new Square(6, 4))); // B
    s = engine.applyMove(s, new SimpleMove(new Square(2, 2), new Square(3, 3))); // W
    s = engine.applyMove(s, new SimpleMove(new Square(1, 5), new Square(0, 4))); // B
    assertThat(s.history()).hasSize(4);
    assertThat(engine.computeStatus(s)).isEqualTo(GameStatus.ONGOING);
  }

  // --- helpers ---

  private static Piece white(PieceKind k) {
    return new Piece(Color.WHITE, k);
  }

  private static Piece black(PieceKind k) {
    return new Piece(Color.BLACK, k);
  }
}
