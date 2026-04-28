package com.damaitaliana.shared.domain;

import java.util.List;
import java.util.Objects;

/**
 * A sequence of one or more jumps over enemy pieces.
 *
 * <p>{@link #from} is the starting square; {@link #path} contains the landing square of every leg
 * (so its size equals the number of jumps); {@link #captured} contains the square of the enemy
 * piece jumped over on the corresponding leg.
 *
 * <p>Invariants:
 *
 * <ul>
 *   <li>{@code path.size() == captured.size() >= 1}.
 *   <li>The captured squares are pairwise distinct (a piece cannot be jumped twice — SPEC §3.4 in
 *       conjunction with the 4 laws of precedence; enforced at generation time as well).
 * </ul>
 */
public record CaptureSequence(Square from, List<Square> path, List<Square> captured)
    implements Move {

  /** Validates invariants and freezes the lists. */
  public CaptureSequence {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(captured, "captured");
    if (path.isEmpty()) {
      throw new IllegalArgumentException("path must contain at least one landing square");
    }
    if (captured.size() != path.size()) {
      throw new IllegalArgumentException(
          "path and captured must have the same size (one captured per leg): path="
              + path.size()
              + ", captured="
              + captured.size());
    }
    if (captured.stream().distinct().count() != captured.size()) {
      throw new IllegalArgumentException("captured squares must be pairwise distinct: " + captured);
    }
    path = List.copyOf(path);
    captured = List.copyOf(captured);
  }

  @Override
  public Square to() {
    return path.get(path.size() - 1);
  }

  @Override
  public List<Square> capturedSquares() {
    return captured;
  }

  /** Number of pieces captured by this sequence. */
  public int captureCount() {
    return captured.size();
  }
}
