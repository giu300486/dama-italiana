package com.damaitaliana.shared.notation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.notation.FidNotation.ParsedMove;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class FidNotationTest {

  // --- bijection: anchor cases (corners and adjacent rows) ---

  @ParameterizedTest(name = "FID {0} = ({1},{2})")
  @CsvSource({
    "1, 1, 7",
    "2, 3, 7",
    "3, 5, 7",
    "4, 7, 7",
    "5, 0, 6",
    "8, 6, 6",
    "9, 1, 5",
    "12, 7, 5",
    "13, 0, 4",
    "16, 6, 4",
    "20, 7, 3",
    "21, 0, 2",
    "24, 6, 2",
    "28, 7, 1",
    "29, 0, 0",
    "30, 2, 0",
    "31, 4, 0",
    "32, 6, 0"
  })
  void toSquareReturnsExpectedCoordinates(int fid, int file, int rank) {
    assertThat(FidNotation.toSquare(fid)).isEqualTo(new Square(file, rank));
  }

  @Test
  void bijectionIsExhaustiveOverAll32Squares() {
    for (int n = FidNotation.MIN; n <= FidNotation.MAX; n++) {
      Square s = FidNotation.toSquare(n);
      assertThat(s.isDark()).as("square %d must be dark", n).isTrue();
      assertThat(FidNotation.toFid(s)).as("round-trip square %d", n).isEqualTo(n);
    }
  }

  @Test
  void bijectionCoversAll32DarkSquaresExactlyOnce() {
    java.util.Set<Square> seen = new java.util.HashSet<>();
    for (int n = FidNotation.MIN; n <= FidNotation.MAX; n++) {
      seen.add(FidNotation.toSquare(n));
    }
    assertThat(seen).hasSize(32);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, 33, 100, Integer.MIN_VALUE, Integer.MAX_VALUE})
  void toSquareRejectsOutOfRange(int n) {
    assertThatThrownBy(() -> FidNotation.toSquare(n))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("[1,32]");
  }

  @Test
  void toFidRejectsLightSquare() {
    Square light = new Square(0, 1); // (0+1)%2 == 1 → light
    assertThat(light.isDark()).isFalse();
    assertThatThrownBy(() -> FidNotation.toFid(light))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("light");
  }

  // --- move parsing ---

  @Test
  void parsesSimpleMove() {
    ParsedMove m = FidNotation.parseMove("12-16");
    assertThat(m.capture()).isFalse();
    assertThat(m.squares()).containsExactly(12, 16);
    assertThat(m.from()).isEqualTo(12);
    assertThat(m.to()).isEqualTo(16);
  }

  @Test
  void parsesSingleCapture() {
    ParsedMove m = FidNotation.parseMove("12x19");
    assertThat(m.capture()).isTrue();
    assertThat(m.squares()).containsExactly(12, 19);
  }

  @Test
  void parsesMultiCapture() {
    ParsedMove m = FidNotation.parseMove("12x19x26");
    assertThat(m.capture()).isTrue();
    assertThat(m.squares()).containsExactly(12, 19, 26);
    assertThat(m.from()).isEqualTo(12);
    assertThat(m.to()).isEqualTo(26);
  }

  @Test
  void parseTrimsLeadingAndTrailingWhitespace() {
    assertThat(FidNotation.parseMove("  12-16  ").squares()).containsExactly(12, 16);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "   ",
        "12",
        "12-",
        "-12",
        "12-16-20",
        "12x16-20",
        "12-x16",
        "12--16",
        "12xx16",
        "abc",
        "12-abc",
        "12-0",
        "0-12",
        "33-1",
        "12-33"
      })
  void parseRejectsMalformedOrOutOfRangeMoves(String text) {
    assertThatThrownBy(() -> FidNotation.parseMove(text))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void parseRejectsNullText() {
    assertThatThrownBy(() -> FidNotation.parseMove(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("null");
  }

  @Test
  void parseRejectsDuplicateConsecutiveSquares() {
    assertThatThrownBy(() -> FidNotation.parseMove("12x12"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("duplicate");
  }

  // --- move formatting ---

  @Test
  void formatsSimpleMove() {
    assertThat(FidNotation.formatMove(List.of(12, 16), false)).isEqualTo("12-16");
  }

  @Test
  void formatsSingleCapture() {
    assertThat(FidNotation.formatMove(List.of(12, 19), true)).isEqualTo("12x19");
  }

  @Test
  void formatsMultiCapture() {
    assertThat(FidNotation.formatMove(List.of(12, 19, 26, 30), true)).isEqualTo("12x19x26x30");
  }

  @Test
  void formatRejectsSimpleMoveWithMoreThanTwoSquares() {
    assertThatThrownBy(() -> FidNotation.formatMove(List.of(12, 16, 20), false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("simple");
  }

  @Test
  void formatRejectsTooShortPath() {
    assertThatThrownBy(() -> FidNotation.formatMove(List.of(12), true))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> FidNotation.formatMove(List.of(), true))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void formatRejectsOutOfRangeSquare() {
    assertThatThrownBy(() -> FidNotation.formatMove(List.of(12, 33), true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("[1,32]");
  }

  // --- ParsedMove invariants ---

  @Test
  void parsedMoveIsImmutable() {
    java.util.List<Integer> mutable = new java.util.ArrayList<>(List.of(12, 19, 26));
    ParsedMove m = new ParsedMove(mutable, true);
    mutable.add(99);
    assertThat(m.squares()).containsExactly(12, 19, 26);
  }

  @Test
  void parsedMoveRejectsTooShortPath() {
    assertThatThrownBy(() -> new ParsedMove(List.of(), true))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ParsedMove(List.of(12), true))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // --- round trip parse → format ---

  @Test
  void roundTripSimpleMove() {
    ParsedMove m = FidNotation.parseMove("21-17");
    assertThat(FidNotation.formatMove(m.squares(), m.capture())).isEqualTo("21-17");
  }

  @Test
  void roundTripMultiCapture() {
    ParsedMove m = FidNotation.parseMove("12x19x26x30");
    assertThat(FidNotation.formatMove(m.squares(), m.capture())).isEqualTo("12x19x26x30");
  }
}
