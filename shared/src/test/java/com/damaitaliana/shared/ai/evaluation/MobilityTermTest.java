package com.damaitaliana.shared.ai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.Square;
import java.util.List;
import org.junit.jupiter.api.Test;

class MobilityTermTest {

  private final MobilityTerm term = new MobilityTerm();

  @Test
  void initialPositionIsBalancedMobility() {
    GameState start = GameState.initial();
    assertThat(term.score(start, Color.WHITE)).isZero();
    assertThat(term.score(start, Color.BLACK)).isZero();
  }

  @Test
  void scoreIsPositiveWhenPerspectiveHasMoreMoves() {
    // White king on (3,3) has 4 diagonal moves. Black man on (7,7) has only 1.
    Board b =
        Board.empty()
            .with(new Square(3, 3), new Piece(Color.WHITE, PieceKind.KING))
            .with(new Square(7, 7), new Piece(Color.BLACK, PieceKind.MAN));
    GameState s = ongoingState(b, Color.WHITE);
    int whiteScore = term.score(s, Color.WHITE);
    int blackScore = term.score(s, Color.BLACK);
    assertThat(whiteScore).isPositive();
    assertThat(whiteScore).isEqualTo(-blackScore);
  }

  @Test
  void perspectiveSwapFlipsSign() {
    Board b =
        Board.empty()
            .with(new Square(0, 2), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(7, 5), new Piece(Color.BLACK, PieceKind.MAN));
    GameState s = ongoingState(b, Color.WHITE);
    assertThat(term.score(s, Color.WHITE)).isEqualTo(-term.score(s, Color.BLACK));
  }

  @Test
  void rejectsNullRuleEngine() {
    assertThatThrownBy(() -> new MobilityTerm(null)).isInstanceOf(NullPointerException.class);
  }

  private static GameState ongoingState(Board b, Color sideToMove) {
    return new GameState(b, sideToMove, 0, List.of(), GameStatus.ONGOING);
  }
}
