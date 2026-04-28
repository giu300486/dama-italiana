package com.damaitaliana.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameStateTest {

  @Test
  void initialStateHasInitialBoardWhiteToMoveOngoing() {
    GameState s = GameState.initial();
    assertThat(s.board()).isEqualTo(Board.initial());
    assertThat(s.sideToMove()).isEqualTo(Color.WHITE);
    assertThat(s.halfmoveClock()).isZero();
    assertThat(s.history()).isEmpty();
    assertThat(s.status()).isEqualTo(GameStatus.ONGOING);
  }

  @Test
  void historyIsImmutableEvenIfSourceListMutated() {
    List<Move> source = new ArrayList<>();
    source.add(new SimpleMove(new Square(0, 0), new Square(1, 1)));
    GameState s = new GameState(Board.empty(), Color.WHITE, 0, source, GameStatus.ONGOING);
    source.add(new SimpleMove(new Square(2, 2), new Square(3, 3)));
    assertThat(s.history()).hasSize(1);
  }

  @Test
  void rejectsNegativeHalfmoveClock() {
    assertThatThrownBy(
            () -> new GameState(Board.empty(), Color.WHITE, -1, List.of(), GameStatus.ONGOING))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullFields() {
    assertThatThrownBy(() -> new GameState(null, Color.WHITE, 0, List.of(), GameStatus.ONGOING))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new GameState(Board.empty(), null, 0, List.of(), GameStatus.ONGOING))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new GameState(Board.empty(), Color.WHITE, 0, null, GameStatus.ONGOING))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new GameState(Board.empty(), Color.WHITE, 0, List.of(), null))
        .isInstanceOf(NullPointerException.class);
  }

  // --- GameStatus helpers ---

  @Test
  void statusHelpersClassifyEachValueExactlyOnce() {
    for (GameStatus status : GameStatus.values()) {
      int hits = 0;
      if (status.isOngoing()) hits++;
      if (status.isWin()) hits++;
      if (status.isDraw()) hits++;
      assertThat(hits).as("status %s", status).isEqualTo(1);
    }
  }

  @Test
  void winsAreOnlyWhiteWinsAndBlackWins() {
    assertThat(GameStatus.WHITE_WINS.isWin()).isTrue();
    assertThat(GameStatus.BLACK_WINS.isWin()).isTrue();
    assertThat(GameStatus.DRAW_REPETITION.isWin()).isFalse();
  }

  @Test
  void drawCoversAllThreeReasons() {
    assertThat(GameStatus.DRAW_REPETITION.isDraw()).isTrue();
    assertThat(GameStatus.DRAW_FORTY_MOVES.isDraw()).isTrue();
    assertThat(GameStatus.DRAW_AGREEMENT.isDraw()).isTrue();
    assertThat(GameStatus.WHITE_WINS.isDraw()).isFalse();
  }
}
