package com.damaitaliana.shared.domain;

import java.util.Objects;

/** A piece on the board: a colour and a kind. */
public record Piece(Color color, PieceKind kind) {

  /** Validates non-null fields. */
  public Piece {
    Objects.requireNonNull(color, "color");
    Objects.requireNonNull(kind, "kind");
  }

  public boolean isMan() {
    return kind == PieceKind.MAN;
  }

  public boolean isKing() {
    return kind == PieceKind.KING;
  }

  /**
   * Returns this piece promoted to king. SPEC §3.5.
   *
   * @throws IllegalStateException if the piece is already a king.
   */
  public Piece promote() {
    if (kind == PieceKind.KING) {
      throw new IllegalStateException("piece is already a king");
    }
    return new Piece(color, PieceKind.KING);
  }
}
