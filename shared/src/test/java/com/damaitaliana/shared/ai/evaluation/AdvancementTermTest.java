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

class AdvancementTermTest {

  private final AdvancementTerm term = new AdvancementTerm();

  @Test
  void initialPositionHasZeroAdvancement() {
    // White men: 4 on rank 0 (adv 0) + 4 on rank 1 (adv 1) + 4 on rank 2 (adv 2) = 12
    // Black men: 4 on rank 5 (adv 2) + 4 on rank 6 (adv 1) + 4 on rank 7 (adv 0) = 12
    // Difference: 0
    assertThat(term.score(GameState.initial(), Color.WHITE)).isZero();
    assertThat(term.score(GameState.initial(), Color.BLACK)).isZero();
  }

  @Test
  void whiteManFurtherDownTheBoardScoresHigher() {
    // White man on rank 5 has advanced 5 ranks; symmetric black man on rank 2 has advanced 5 ranks.
    Board b =
        Board.empty()
            .with(new Square(0, 0), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(2, 0), new Piece(Color.WHITE, PieceKind.MAN));
    GameState s = stateOf(b);
    // Two white men on rank 0: adv = 0 + 0 = 0. No black men.
    assertThat(term.score(s, Color.WHITE)).isZero();

    Board b2 =
        Board.empty()
            .with(new Square(0, 4), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(2, 0), new Piece(Color.WHITE, PieceKind.MAN));
    GameState s2 = stateOf(b2);
    // adv = 4 + 0 = 4
    assertThat(term.score(s2, Color.WHITE)).isEqualTo(4);
  }

  @Test
  void blackManAdvancementIsMirroredFromRankSeven() {
    Board b = Board.empty().with(new Square(0, 2), new Piece(Color.BLACK, PieceKind.MAN));
    GameState s = stateOf(b);
    // Black on rank 2 has advanced 7-2 = 5 from black's perspective.
    assertThat(term.score(s, Color.BLACK)).isEqualTo(5);
    assertThat(term.score(s, Color.WHITE)).isEqualTo(-5);
  }

  @Test
  void kingsContributeZero() {
    Board b =
        Board.empty()
            .with(new Square(3, 3), new Piece(Color.WHITE, PieceKind.KING))
            .with(new Square(5, 5), new Piece(Color.BLACK, PieceKind.KING));
    GameState s = stateOf(b);
    assertThat(term.score(s, Color.WHITE)).isZero();
    assertThat(term.score(s, Color.BLACK)).isZero();
  }

  @Test
  void perspectiveSwapFlipsSign() {
    Board b =
        Board.empty()
            .with(new Square(0, 4), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(7, 7), new Piece(Color.BLACK, PieceKind.MAN));
    GameState s = stateOf(b);
    assertThat(term.score(s, Color.WHITE)).isEqualTo(-term.score(s, Color.BLACK));
  }

  private static GameState stateOf(Board b) {
    return new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
  }
}
