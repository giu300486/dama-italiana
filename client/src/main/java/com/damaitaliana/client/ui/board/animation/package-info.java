/**
 * Animation engine for the board view.
 *
 * <p>{@link com.damaitaliana.client.ui.board.animation.MoveAnimator} returns the JavaFX {@code
 * Animation} primitives parameterised per SPEC §13.3 (durations, interpolators, axes); {@link
 * com.damaitaliana.client.ui.board.animation.AnimationOrchestrator} composes them for a full {@link
 * com.damaitaliana.shared.domain.Move} — leg-by-leg for capture sequences, with fade-out of the
 * captured piece running in parallel with the slide of the moving piece.
 *
 * <p>Deviation from SPEC §13.3 documented here: the mandatory-capture pulse is implemented with
 * {@link javafx.animation.FadeTransition} on the cell {@link javafx.scene.layout.Region} rather
 * than {@link javafx.animation.FillTransition}. {@code FillTransition} requires a {@link
 * javafx.scene.shape.Shape} target and {@code Region} has no {@code fill} property; fading the
 * cell's opacity produces the equivalent visual pulse on the styled background-color.
 *
 * <p>Wiring of these animations into the {@link
 * com.damaitaliana.client.controller.SinglePlayerController}'s applyMove pipeline is intentionally
 * deferred to Task 3.13, where the AI continuation point makes the async flow natural.
 */
package com.damaitaliana.client.ui.board.animation;
