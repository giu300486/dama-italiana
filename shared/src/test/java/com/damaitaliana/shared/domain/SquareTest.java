package com.damaitaliana.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SquareTest {

  @ParameterizedTest
  @ValueSource(ints = {-1, 8, 100})
  void rejectsFileOutOfRange(int file) {
    assertThatThrownBy(() -> new Square(file, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("file");
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 8, 100})
  void rejectsRankOutOfRange(int rank) {
    assertThatThrownBy(() -> new Square(0, rank))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("rank");
  }

  @ParameterizedTest(name = "({0},{1}) dark={2}")
  @CsvSource({
    "0, 0, true", // SPEC §3.1: dark in white's bottom-left
    "1, 0, false",
    "7, 0, false",
    "6, 0, true",
    "1, 7, true",
    "0, 7, false",
    "7, 7, true",
    "3, 4, false", // (3+4)=7 odd → light
    "4, 4, true" // (4+4)=8 even → dark
  })
  void isDarkFollowsParity(int file, int rank, boolean expected) {
    assertThat(new Square(file, rank).isDark()).isEqualTo(expected);
  }

  @Test
  void exactlyHalfThe64SquaresAreDark() {
    long darkCount = 0;
    for (int rank = 0; rank < 8; rank++) {
      for (int file = 0; file < 8; file++) {
        if (new Square(file, rank).isDark()) {
          darkCount++;
        }
      }
    }
    assertThat(darkCount).isEqualTo(32);
  }

  @Test
  void recordsAreEqualByValue() {
    assertThat(new Square(3, 5)).isEqualTo(new Square(3, 5));
    assertThat(new Square(3, 5)).isNotEqualTo(new Square(5, 3));
  }
}
