package com.damaitaliana.shared.domain;

import java.util.List;

/**
 * A legal move in Italian Draughts.
 *
 * <p>Two flavours, exhaustively enumerated by the sealed interface:
 *
 * <ul>
 *   <li>{@link SimpleMove} — a single sliding step, no capture.
 *   <li>{@link CaptureSequence} — one or more jumps over enemy pieces.
 * </ul>
 *
 * <p>SPEC §3.2 / §3.3 / §3.4 / §3.8.
 */
public sealed interface Move permits SimpleMove, CaptureSequence {

  /** The square the moving piece starts from. */
  Square from();

  /** The square the moving piece ends on (after all jumps, if any). */
  Square to();

  /**
   * The squares of the pieces captured during this move, in jump order. Empty for {@link
   * SimpleMove}.
   */
  List<Square> capturedSquares();

  /** True when this move captures at least one enemy piece. */
  default boolean isCapture() {
    return !capturedSquares().isEmpty();
  }
}
