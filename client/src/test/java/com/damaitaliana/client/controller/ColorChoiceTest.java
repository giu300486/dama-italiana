package com.damaitaliana.client.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.damaitaliana.shared.domain.Color;
import java.util.HashSet;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class ColorChoiceTest {

  @Test
  void whiteResolvesToWhite() {
    assertThat(ColorChoice.WHITE.resolve(new SplittableRandom(0L))).isEqualTo(Color.WHITE);
  }

  @Test
  void blackResolvesToBlack() {
    assertThat(ColorChoice.BLACK.resolve(new SplittableRandom(0L))).isEqualTo(Color.BLACK);
  }

  @Test
  void randomResolvesToOneOfTheTwoColors() {
    RandomGenerator rng = new SplittableRandom(42L);
    Color color = ColorChoice.RANDOM.resolve(rng);
    assertThat(color).isIn(Color.WHITE, Color.BLACK);
  }

  @Test
  void randomEventuallyEmitsBothColorsAcrossManySeeds() {
    var seen = new HashSet<Color>();
    for (long seed = 0; seed < 50 && seen.size() < 2; seed++) {
      seen.add(ColorChoice.RANDOM.resolve(new SplittableRandom(seed)));
    }
    assertThat(seen).containsExactlyInAnyOrder(Color.WHITE, Color.BLACK);
  }

  @Test
  void resolveWithNullRngThrows() {
    assertThatNullPointerException().isThrownBy(() -> ColorChoice.RANDOM.resolve(null));
  }

  @Test
  void deterministicAcrossSameSeed() {
    Color a = ColorChoice.RANDOM.resolve(new SplittableRandom(123L));
    Color b = ColorChoice.RANDOM.resolve(new SplittableRandom(123L));
    assertThat(a).isEqualTo(b);
  }
}
