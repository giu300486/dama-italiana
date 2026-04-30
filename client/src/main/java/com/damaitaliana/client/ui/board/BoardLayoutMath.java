package com.damaitaliana.client.ui.board;

import com.damaitaliana.shared.domain.Square;

/**
 * Pure helpers for board geometry: dark-cell test, cell pixel size given the available area, and
 * file/rank → screen coordinate conversion. Kept separate from {@link BoardRenderer} so unit tests
 * can verify the math without booting the JavaFX toolkit.
 */
public final class BoardLayoutMath {

  public static final int BOARD_SIZE = 8;

  private BoardLayoutMath() {}

  /**
   * Italian Draughts plays only on dark squares. With file 0 / rank 0 as the bottom-left dark
   * square (SPEC §3.1), a square is dark when {@code file + rank} is even.
   */
  public static boolean isDarkSquare(Square s) {
    return (s.file() + s.rank()) % 2 == 0;
  }

  /** Cell side length in pixels for a render area of {@code width × height}. */
  public static double cellSize(double width, double height) {
    double smaller = Math.min(width, height);
    return smaller / BOARD_SIZE;
  }

  /** X coordinate of the cell at {@code file}'s left edge. */
  public static double xFor(int file, double cellSize) {
    return file * cellSize;
  }

  /**
   * Y coordinate of the cell at {@code rank}'s top edge. Rank 0 is at the bottom from White's
   * perspective; JavaFX's y-axis is top-down, so the conversion mirrors the rank.
   */
  public static double yFor(int rank, double cellSize) {
    return (BOARD_SIZE - 1 - rank) * cellSize;
  }
}
