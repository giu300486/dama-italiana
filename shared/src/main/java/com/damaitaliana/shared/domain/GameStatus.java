package com.damaitaliana.shared.domain;

/**
 * Status of a game.
 *
 * <p>SPEC §8.1 lists four values: {@code ONGOING}, {@code WHITE_WINS}, {@code BLACK_WINS}, {@code
 * DRAW}. We expand the {@code DRAW} branch into the three legal draw reasons so that the UI can
 * display the cause of the draw and tests can assert on the exact mechanism. External behaviour
 * stays the same — use {@link #isDraw()} to check the union.
 *
 * <p>Note: in Italian Draughts a stalemate (no legal moves for the side to move) is a loss for the
 * stalemated player, not a draw (SPEC §3.6). There is therefore no stalemate-draw value.
 */
public enum GameStatus {

  /** The game is still being played. */
  ONGOING,

  /** White has won. */
  WHITE_WINS,

  /** Black has won. */
  BLACK_WINS,

  /** Draw by threefold repetition (SPEC §3.6). */
  DRAW_REPETITION,

  /** Draw by the 40-move rule (SPEC §3.6). */
  DRAW_FORTY_MOVES,

  /** Draw by mutual agreement (SPEC §3.6). Triggered by the UI/network layer, not by F1. */
  DRAW_AGREEMENT;

  public boolean isOngoing() {
    return this == ONGOING;
  }

  public boolean isWin() {
    return this == WHITE_WINS || this == BLACK_WINS;
  }

  public boolean isDraw() {
    return this == DRAW_REPETITION || this == DRAW_FORTY_MOVES || this == DRAW_AGREEMENT;
  }
}
