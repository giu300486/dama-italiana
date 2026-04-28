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

class CenterControlTermTest {

  private final CenterControlTerm term = new CenterControlTerm();

  @Test
  void initialPositionIsZero() {
    // Centre squares (rank 3-4) are empty in the initial position.
    assertThat(term.score(GameState.initial(), Color.WHITE)).isZero();
    assertThat(term.score(GameState.initial(), Color.BLACK)).isZero();
  }

  @Test
  void centerSquaresAreFidFourteenFifteenEighteenNineteen() {
    assertThat(CenterControlTerm.CENTER_SQUARES)
        .containsExactlyInAnyOrder(
            new Square(2, 4), new Square(4, 4), new Square(3, 3), new Square(5, 3));
  }

  @Test
  void friendlyPieceOnCenterScoresPlusOne() {
    Board b = Board.empty().with(new Square(3, 3), new Piece(Color.WHITE, PieceKind.KING));
    GameState s = stateOf(b);
    assertThat(term.score(s, Color.WHITE)).isEqualTo(1);
    assertThat(term.score(s, Color.BLACK)).isEqualTo(-1);
  }

  @Test
  void opponentPieceOnCenterScoresMinusOne() {
    Board b = Board.empty().with(new Square(4, 4), new Piece(Color.BLACK, PieceKind.MAN));
    GameState s = stateOf(b);
    assertThat(term.score(s, Color.WHITE)).isEqualTo(-1);
  }

  @Test
  void piecesOffCenterContributeZero() {
    Board b =
        Board.empty()
            .with(new Square(0, 0), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(7, 7), new Piece(Color.BLACK, PieceKind.MAN));
    GameState s = stateOf(b);
    assertThat(term.score(s, Color.WHITE)).isZero();
  }

  @Test
  void allFourCenterSquaresAccumulate() {
    Board b =
        Board.empty()
            .with(new Square(2, 4), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(4, 4), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(3, 3), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(5, 3), new Piece(Color.BLACK, PieceKind.KING));
    GameState s = stateOf(b);
    assertThat(term.score(s, Color.WHITE)).isEqualTo(3 - 1);
  }

  private static GameState stateOf(Board b) {
    return new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
  }
}
