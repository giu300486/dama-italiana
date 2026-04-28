package com.damaitaliana.shared.domain;

/** The two players. SPEC §3.1: White moves first. */
public enum Color {
  WHITE,
  BLACK;

  /** Returns the opponent of this color. */
  public Color opposite() {
    return this == WHITE ? BLACK : WHITE;
  }
}
