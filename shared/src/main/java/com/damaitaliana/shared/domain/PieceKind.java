package com.damaitaliana.shared.domain;

/**
 * The two kinds of piece in Italian Draughts.
 *
 * <ul>
 *   <li>{@link #MAN} — pedina; moves and captures only forward (SPEC §3.2, §3.3).
 *   <li>{@link #KING} — dama; moves one square in any of the four diagonals (SPEC §3.2). The
 *       Italian variant explicitly forbids the "flying king" of International Draughts.
 * </ul>
 */
public enum PieceKind {
  MAN,
  KING
}
