package com.damaitaliana.shared.domain;

import java.util.List;
import java.util.Objects;

/**
 * The complete state of a game at one point in time. Immutable.
 *
 * @param board current piece positions.
 * @param sideToMove the player whose turn it is.
 * @param halfmoveClock number of half-moves since the last capture or pawn move (SPEC §3.6 "regola
 *     delle 40 mosse"). Reset to 0 when a capture or man move happens; incremented otherwise.
 * @param history every move ever applied, in order. Used both for replay and for repetition
 *     detection (SPEC §3.6).
 * @param status game status. {@link GameStatus#ONGOING} on a fresh game.
 */
public record GameState(
    Board board, Color sideToMove, int halfmoveClock, List<Move> history, GameStatus status) {

  /** Validates fields and freezes the history. */
  public GameState {
    Objects.requireNonNull(board, "board");
    Objects.requireNonNull(sideToMove, "sideToMove");
    Objects.requireNonNull(history, "history");
    Objects.requireNonNull(status, "status");
    if (halfmoveClock < 0) {
      throw new IllegalArgumentException("halfmoveClock must be non-negative: " + halfmoveClock);
    }
    history = List.copyOf(history);
  }

  /** A fresh game, white to move, empty history, ongoing. */
  public static GameState initial() {
    return new GameState(Board.initial(), Color.WHITE, 0, List.of(), GameStatus.ONGOING);
  }
}
