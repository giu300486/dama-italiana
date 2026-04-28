package com.damaitaliana.shared.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.domain.Board;
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

/** Promotion (SPEC §3.5) and halfmove clock (SPEC §3.6) edge cases on {@code applyMove}. */
class ItalianRuleEngineApplyMoveTest {

  private final ItalianRuleEngine engine = new ItalianRuleEngine();

  @Test
  void whiteManSimpleMoveToRank7Promotes() {
    Board b = Board.empty().with(new Square(2, 6), white(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    SimpleMove move = new SimpleMove(new Square(2, 6), new Square(3, 7));
    GameState after = engine.applyMove(s, move);
    assertThat(after.board().at(new Square(3, 7))).contains(white(PieceKind.KING));
  }

  @Test
  void blackManSimpleMoveToRank0Promotes() {
    Board b = Board.empty().with(new Square(3, 1), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.BLACK, 0, List.of(), GameStatus.ONGOING);
    SimpleMove move = new SimpleMove(new Square(3, 1), new Square(2, 0));
    GameState after = engine.applyMove(s, move);
    assertThat(after.board().at(new Square(2, 0))).contains(black(PieceKind.KING));
  }

  @Test
  void manCaptureLandingOnPromotionRowPromotes() {
    // White man at (1,5) captures black man at (2,6) and lands at (3,7) → promoted.
    Board b =
        Board.empty()
            .with(new Square(1, 5), white(PieceKind.MAN))
            .with(new Square(2, 6), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    Move move = engine.legalMoves(s).get(0);
    GameState after = engine.applyMove(s, move);
    assertThat(after.board().at(new Square(3, 7))).contains(white(PieceKind.KING));
  }

  @Test
  void kingMoveToPromotionRowDoesNotChangeKind() {
    Board b = Board.empty().with(new Square(2, 6), white(PieceKind.KING));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    SimpleMove move = new SimpleMove(new Square(2, 6), new Square(3, 7));
    GameState after = engine.applyMove(s, move);
    assertThat(after.board().at(new Square(3, 7))).contains(white(PieceKind.KING));
  }

  @Test
  void kingSimpleMoveAwayFromPromotionRowKeepsClockIncrementing() {
    Board b = Board.empty().with(new Square(2, 4), white(PieceKind.KING));
    GameState s = new GameState(b, Color.WHITE, 13, List.of(), GameStatus.ONGOING);
    SimpleMove move = new SimpleMove(new Square(2, 4), new Square(3, 5));
    GameState after = engine.applyMove(s, move);
    assertThat(after.halfmoveClock()).isEqualTo(14);
  }

  @Test
  void manMoveLandingOnPromotionRowResetsClock() {
    Board b = Board.empty().with(new Square(2, 6), white(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 23, List.of(), GameStatus.ONGOING);
    SimpleMove move = new SimpleMove(new Square(2, 6), new Square(3, 7));
    GameState after = engine.applyMove(s, move);
    assertThat(after.halfmoveClock()).isZero();
  }

  @Test
  void historyAccumulatesAcrossMoves() {
    Board b =
        Board.empty()
            .with(new Square(2, 4), white(PieceKind.KING))
            .with(new Square(0, 4), black(PieceKind.KING));
    GameState s0 = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    Move m1 = new SimpleMove(new Square(2, 4), new Square(3, 5));
    GameState s1 = engine.applyMove(s0, m1);
    Move m2 = new SimpleMove(new Square(0, 4), new Square(1, 5));
    GameState s2 = engine.applyMove(s1, m2);
    assertThat(s2.history()).containsExactly(m1, m2);
    assertThat(s2.sideToMove()).isEqualTo(Color.WHITE);
  }

  // --- helpers ---

  private static Piece white(PieceKind k) {
    return new Piece(Color.WHITE, k);
  }

  private static Piece black(PieceKind k) {
    return new Piece(Color.BLACK, k);
  }
}
