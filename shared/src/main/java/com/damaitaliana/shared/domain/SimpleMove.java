package com.damaitaliana.shared.domain;

import java.util.List;
import java.util.Objects;

/** A non-capturing diagonal step. */
public record SimpleMove(Square from, Square to) implements Move {

  /** Validates non-null endpoints. */
  public SimpleMove {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    if (from.equals(to)) {
      throw new IllegalArgumentException("from and to must differ");
    }
  }

  @Override
  public List<Square> capturedSquares() {
    return List.of();
  }
}
