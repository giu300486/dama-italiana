/**
 * Board view: renderer, side panel and animation engine.
 *
 * <p>The renderer ({@link com.damaitaliana.client.ui.board.BoardRenderer}) is a custom JavaFX
 * {@link javafx.scene.layout.Region} of 64 cell tiles plus a {@code PieceNode} per occupied square.
 * It exposes a small API the controllers in {@code com.damaitaliana.client.controller} drive:
 * {@code renderState}, {@code highlightLegalTargets}, {@code highlightMandatorySources}, {@code
 * clearHighlights}, and {@code snapshot} (used by save miniatures and rules diagrams).
 *
 * <p>Layout rule: file 0 / rank 0 is the bottom-left dark square from White's perspective (SPEC
 * §3.1). JavaFX's y-axis is top-down so rank 0 lives at {@code (BOARD_SIZE - 1) * cellSize}.
 *
 * <p>The pure-logic helpers ({@link com.damaitaliana.client.ui.board.BoardLayoutMath}, {@link
 * com.damaitaliana.client.ui.board.HighlightState}, {@link
 * com.damaitaliana.client.ui.board.PieceAccessibleText}) are in this package so unit tests can
 * exercise the renderer's behaviour without booting the JavaFX toolkit; full end-to-end coverage
 * comes from Task 3.21 TestFX.
 */
package com.damaitaliana.client.ui.board;
