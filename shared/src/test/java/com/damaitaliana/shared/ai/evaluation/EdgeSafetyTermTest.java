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

class EdgeSafetyTermTest {

  private final EdgeSafetyTerm term = new EdgeSafetyTerm();

  @Test
  void initialPositionIsBalanced() {
    // White edge pieces (file=0 or 7): (0,0), (0,2), (7,1) — 3 pieces.
    // Black edge pieces: (7,5), (0,6), (7,7) — 3 pieces.
    assertThat(term.score(GameState.initial(), Color.WHITE)).isZero();
    assertThat(term.score(GameState.initial(), Color.BLACK)).isZero();
  }

  @Test
  void onlyEdgeFilesContribute() {
    Board b =
        Board.empty()
            .with(new Square(0, 0), new Piece(Color.WHITE, PieceKind.MAN)) // edge
            .with(new Square(2, 0), new Piece(Color.WHITE, PieceKind.MAN)) // not edge
            .with(new Square(4, 4), new Piece(Color.WHITE, PieceKind.KING)); // not edge
    GameState s = stateOf(b);
    assertThat(term.score(s, Color.WHITE)).isEqualTo(1);
  }

  @Test
  void opponentEdgePiecesSubtract() {
    Board b =
        Board.empty()
            .with(new Square(0, 0), new Piece(Color.WHITE, PieceKind.MAN)) // +1
            .with(new Square(7, 7), new Piece(Color.BLACK, PieceKind.MAN)) // -1
            .with(new Square(7, 1), new Piece(Color.BLACK, PieceKind.KING)); // -1
    GameState s = stateOf(b);
    assertThat(term.score(s, Color.WHITE)).isEqualTo(1 - 2);
  }

  @Test
  void perspectiveSwapFlipsSign() {
    Board b =
        Board.empty()
            .with(new Square(0, 0), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(7, 7), new Piece(Color.BLACK, PieceKind.KING));
    GameState s = stateOf(b);
    assertThat(term.score(s, Color.WHITE)).isEqualTo(-term.score(s, Color.BLACK));
  }

  private static GameState stateOf(Board b) {
    return new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
  }
}
