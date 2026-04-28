package com.damaitaliana.shared.ai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.Square;
import java.util.List;
import org.junit.jupiter.api.Test;

class MaterialTermTest {

  private final MaterialTerm term = new MaterialTerm();

  // --- balanced positions ---

  @Test
  void scoreIsZeroOnInitialPosition() {
    GameState start = GameState.initial();
    assertThat(term.score(start, Color.WHITE)).isZero();
    assertThat(term.score(start, Color.BLACK)).isZero();
  }

  @Test
  void scoreIsZeroOnEmptyBoard() {
    GameState empty = stateOf(Board.empty());
    assertThat(term.score(empty, Color.WHITE)).isZero();
  }

  // --- imbalanced material ---

  @Test
  void scoreIsPositiveForPerspectiveWithMoreMen() {
    Board b =
        Board.empty()
            .with(new Square(0, 0), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(2, 0), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(7, 7), new Piece(Color.BLACK, PieceKind.MAN));
    GameState s = stateOf(b);
    assertThat(term.score(s, Color.WHITE)).isEqualTo(MaterialTerm.MAN_VALUE);
    assertThat(term.score(s, Color.BLACK)).isEqualTo(-MaterialTerm.MAN_VALUE);
  }

  @Test
  void kingIsWorthThreeMen() {
    Board b =
        Board.empty()
            .with(new Square(0, 0), new Piece(Color.BLACK, PieceKind.KING))
            .with(new Square(2, 0), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(4, 0), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(6, 0), new Piece(Color.WHITE, PieceKind.MAN));
    GameState s = stateOf(b);
    // Black has 1 king (300) vs White's 3 men (300) → balanced from either side.
    assertThat(term.score(s, Color.WHITE)).isZero();
    assertThat(term.score(s, Color.BLACK)).isZero();
  }

  @Test
  void scoreIsAccurateWhenBothSidesHaveKingsAndMen() {
    // White:  king(+300) + man(+100) = +400
    // Black:  king(-300) + king(-300) = -600
    // Total from White's perspective: -200
    Board b =
        Board.empty()
            .with(new Square(0, 0), new Piece(Color.WHITE, PieceKind.KING))
            .with(new Square(2, 0), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(7, 7), new Piece(Color.BLACK, PieceKind.KING))
            .with(new Square(5, 7), new Piece(Color.BLACK, PieceKind.KING));
    GameState s = stateOf(b);
    assertThat(term.score(s, Color.WHITE)).isEqualTo(-200);
    assertThat(term.score(s, Color.BLACK)).isEqualTo(200);
  }

  // --- perspective ---

  @Test
  void perspectiveFlipsSign() {
    Board b =
        Board.empty()
            .with(new Square(0, 0), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(2, 0), new Piece(Color.WHITE, PieceKind.KING))
            .with(new Square(7, 7), new Piece(Color.BLACK, PieceKind.KING));
    GameState s = stateOf(b);
    int white = term.score(s, Color.WHITE);
    int black = term.score(s, Color.BLACK);
    assertThat(white).isEqualTo(-black);
  }

  // --- helper ---

  private static GameState stateOf(Board b) {
    return new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
  }
}
