package com.damaitaliana.client.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class GameSessionTest {

  private final GameSession session = new GameSession();
  private final SinglePlayerGame sample =
      new SinglePlayerGame(
          AiLevel.ESPERTO, Color.WHITE, "Partita", GameState.initial(), new SplittableRandom(0L));

  @Test
  void currentGameIsEmptyByDefault() {
    assertThat(session.currentGame()).isEmpty();
  }

  @Test
  void setAndGetCurrentGame() {
    session.setCurrentGame(sample);
    assertThat(session.currentGame()).contains(sample);
  }

  @Test
  void clearRemovesCurrentGame() {
    session.setCurrentGame(sample);
    session.clear();
    assertThat(session.currentGame()).isEmpty();
  }

  @Test
  void setCurrentGameRejectsNull() {
    assertThatNullPointerException().isThrownBy(() -> session.setCurrentGame(null));
  }

  @Test
  void setOverwritesPrevious() {
    session.setCurrentGame(sample);
    SinglePlayerGame replacement =
        new SinglePlayerGame(
            AiLevel.CAMPIONE, Color.BLACK, "Altra", GameState.initial(), new SplittableRandom(1L));
    session.setCurrentGame(replacement);
    assertThat(session.currentGame()).contains(replacement);
  }
}
