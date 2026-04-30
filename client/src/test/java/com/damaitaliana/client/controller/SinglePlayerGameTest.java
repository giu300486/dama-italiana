package com.damaitaliana.client.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class SinglePlayerGameTest {

  private final RandomGenerator rng = new SplittableRandom(42L);

  @Test
  void tryCreateSucceedsWithAllFields() {
    Optional<SinglePlayerGame> game =
        SinglePlayerGame.tryCreate(
            AiLevel.ESPERTO, ColorChoice.WHITE, "Partita del 12 aprile", rng);

    assertThat(game).isPresent();
    assertThat(game.get().level()).isEqualTo(AiLevel.ESPERTO);
    assertThat(game.get().humanColor()).isEqualTo(Color.WHITE);
    assertThat(game.get().name()).isEqualTo("Partita del 12 aprile");
    assertThat(game.get().state()).isEqualTo(GameState.initial());
  }

  @Test
  void tryCreateRejectsEmptyName() {
    assertThat(SinglePlayerGame.tryCreate(AiLevel.ESPERTO, ColorChoice.WHITE, "", rng)).isEmpty();
  }

  @Test
  void tryCreateRejectsBlankName() {
    assertThat(SinglePlayerGame.tryCreate(AiLevel.ESPERTO, ColorChoice.WHITE, "   ", rng))
        .isEmpty();
  }

  @Test
  void tryCreateRejectsNullName() {
    assertThat(SinglePlayerGame.tryCreate(AiLevel.ESPERTO, ColorChoice.WHITE, null, rng)).isEmpty();
  }

  @Test
  void tryCreateStripsName() {
    Optional<SinglePlayerGame> game =
        SinglePlayerGame.tryCreate(AiLevel.ESPERTO, ColorChoice.WHITE, "  Partita 1  ", rng);
    assertThat(game).isPresent();
    assertThat(game.get().name()).isEqualTo("Partita 1");
  }

  @Test
  void tryCreateResolvesRandomColorViaRng() {
    var rngBlack = new SplittableRandom(0L);
    var rngWhite = new SplittableRandom(0L);
    while (rngBlack.nextBoolean()) {
      rngBlack = new SplittableRandom(0L);
      break;
    }
    Optional<SinglePlayerGame> game =
        SinglePlayerGame.tryCreate(
            AiLevel.ESPERTO, ColorChoice.RANDOM, "Test", new SplittableRandom(42L));
    assertThat(game).isPresent();
    assertThat(game.get().humanColor()).isIn(Color.WHITE, Color.BLACK);
  }

  @Test
  void constructorRejectsNullFields() {
    GameState s = GameState.initial();
    assertThatNullPointerException()
        .isThrownBy(() -> new SinglePlayerGame(null, Color.WHITE, "n", s, rng));
    assertThatNullPointerException()
        .isThrownBy(() -> new SinglePlayerGame(AiLevel.ESPERTO, null, "n", s, rng));
    assertThatNullPointerException()
        .isThrownBy(() -> new SinglePlayerGame(AiLevel.ESPERTO, Color.WHITE, null, s, rng));
    assertThatNullPointerException()
        .isThrownBy(() -> new SinglePlayerGame(AiLevel.ESPERTO, Color.WHITE, "n", null, rng));
    assertThatNullPointerException()
        .isThrownBy(() -> new SinglePlayerGame(AiLevel.ESPERTO, Color.WHITE, "n", s, null));
  }
}
