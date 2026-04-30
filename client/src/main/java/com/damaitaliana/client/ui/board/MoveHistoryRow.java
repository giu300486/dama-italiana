package com.damaitaliana.client.ui.board;

/**
 * One row of the move history pane. Italian Draughts (like chess) alternates White and Black, so
 * each row carries a turn number and the FID-formatted move for each side. {@link #blackFid} is
 * {@code null} until Black completes the turn.
 */
public record MoveHistoryRow(int moveNumber, String whiteFid, String blackFid) {

  public boolean hasBlackMove() {
    return blackFid != null;
  }
}
