package com.damaitaliana.shared.domain;

/**
 * A coordinate on the 8x8 draughts board.
 *
 * <p>Both {@code file} (column) and {@code rank} (row) are 0-based and bound to {@code [0, 7]}.
 * From White's perspective {@code rank=0} is the row closest to White, {@code rank=7} is the
 * promotion row, {@code file=0} is the leftmost column.
 *
 * <p>Per SPEC §3.1 the dark square sits at White's bottom-left corner, so {@code (0,0)} is dark and
 * the playable squares are exactly those where {@code (file + rank) % 2 == 0}.
 *
 * <p>Introduced as part of Task 1.1 (FID notation utility); extended in Task 1.2 with whatever
 * helpers the rule engine needs.
 */
public record Square(int file, int rank) {

  /** Validates the bounds. */
  public Square {
    if (file < 0 || file > 7) {
      throw new IllegalArgumentException("file out of range [0,7]: " + file);
    }
    if (rank < 0 || rank > 7) {
      throw new IllegalArgumentException("rank out of range [0,7]: " + rank);
    }
  }

  /** True if this square is one of the 32 dark, playable squares (SPEC §3.1). */
  public boolean isDark() {
    return (file + rank) % 2 == 0;
  }
}
