package com.damaitaliana.client.ui.board;

import com.damaitaliana.shared.domain.Color;

/**
 * Snapshot of what the status pane should currently show. Computed by {@link StatusPaneViewModel},
 * consumed by {@link StatusPane}.
 *
 * @param humanLabel localised player line for the human ("You (White)").
 * @param aiLabel localised player line for the opponent ("AI Expert (Black)").
 * @param turnLabel localised "X's turn" line; {@code null} when the game is over.
 * @param endgameLabel localised endgame line ("White wins", "Draw by …"); {@code null} while
 *     ongoing.
 * @param sideToMove side currently to move; the chip colour reflects this. When the game is over
 *     this still holds the side that would have moved next, used as a fallback.
 * @param ended true when the game has reached a terminal status.
 */
public record StatusPaneState(
    String humanLabel,
    String aiLabel,
    String turnLabel,
    String endgameLabel,
    Color sideToMove,
    boolean ended) {}
