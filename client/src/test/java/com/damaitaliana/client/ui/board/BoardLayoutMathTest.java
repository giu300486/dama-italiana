package com.damaitaliana.client.ui.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.damaitaliana.shared.domain.Square;
import org.junit.jupiter.api.Test;

class BoardLayoutMathTest {

  @Test
  void cellSizeShrinksToSmallerDimension() {
    assertThat(BoardLayoutMath.cellSize(800, 600)).isEqualTo(75);
    assertThat(BoardLayoutMath.cellSize(640, 800)).isEqualTo(80);
    assertThat(BoardLayoutMath.cellSize(640, 640)).isEqualTo(80);
  }

  @Test
  void xForReturnsLeftEdgeOfFile() {
    assertThat(BoardLayoutMath.xFor(0, 80)).isEqualTo(0);
    assertThat(BoardLayoutMath.xFor(7, 80)).isEqualTo(560);
  }

  @Test
  void yForMirrorsRankBecauseJavaFxYIsTopDown() {
    // Rank 0 is bottom-left from white's view; in JavaFX y-axis grows downward,
    // so rank 0 sits at y = 7 * cellSize and rank 7 sits at y = 0.
    assertThat(BoardLayoutMath.yFor(0, 80)).isEqualTo(560);
    assertThat(BoardLayoutMath.yFor(7, 80)).isEqualTo(0);
    assertThat(BoardLayoutMath.yFor(3, 80)).isEqualTo(320);
  }

  @Test
  void darkSquareIsBottomLeftFromWhiteView() {
    // SPEC §3.1: bottom-left dark from white. Square(0,0) must be dark.
    assertThat(BoardLayoutMath.isDarkSquare(new Square(0, 0))).isTrue();
    assertThat(BoardLayoutMath.isDarkSquare(new Square(1, 0))).isFalse();
    assertThat(BoardLayoutMath.isDarkSquare(new Square(0, 1))).isFalse();
    assertThat(BoardLayoutMath.isDarkSquare(new Square(1, 1))).isTrue();
  }

  @Test
  void exactlyHalfTheBoardIsDark() {
    int dark = 0;
    for (int file = 0; file < BoardLayoutMath.BOARD_SIZE; file++) {
      for (int rank = 0; rank < BoardLayoutMath.BOARD_SIZE; rank++) {
        if (BoardLayoutMath.isDarkSquare(new Square(file, rank))) {
          dark++;
        }
      }
    }
    assertThat(dark).isEqualTo(32);
  }

  @Test
  void cellSizeIsExactlyOneEighthOfTheSmallerSide() {
    double size = BoardLayoutMath.cellSize(1024, 768);
    assertThat(size).isEqualTo(96, within(1e-9));
    assertThat(size * BoardLayoutMath.BOARD_SIZE).isEqualTo(768, within(1e-9));
  }
}
