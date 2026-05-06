/**
 * Programmatic layout helpers introduced by Fase 4.5 (responsive desktop, ADR-043, SPEC §13.7,
 * NFR-U-05).
 *
 * <p>JavaFX 21 CSS does not natively support {@code clamp(min, vw, max)} sizing or proportional
 * paddings tied to a sibling region's bounds, so the responsive layer keeps the visual side of the
 * F3.5 design system intact (anti-pattern CLAUDE.md §8 #15 — token CSS v2, texture wood, font
 * families, animation parameters all unchanged) while adding pure-math utilities + scene-walking
 * binders that hook into JavaFX property bindings:
 *
 * <ul>
 *   <li>{@link com.damaitaliana.client.layout.JavaFxScalingHelper} — fluid display typography for
 *       labels carrying the {@code display-fluid} / {@code display-fluid-lg} marker classes (Task
 *       4.5.7), invoked by {@link com.damaitaliana.client.app.SceneRouter} after every navigation.
 *   <li>{@link com.damaitaliana.client.layout.BoardFrameThicknessHelper} — proportional padding
 *       around the {@code BoardRenderer} so the wood frame thickness scales with board side instead
 *       of a fixed 24 px (Task 4.5.8), wired from {@link
 *       com.damaitaliana.client.ui.board.BoardViewController}.
 * </ul>
 *
 * <p>Each helper splits a pure-math static method (testable headless) from the binding entry point
 * (requires the JavaFX toolkit). The bind-side is idempotent — unbind first, then bind — so
 * repeated invocations from different lifecycle hooks (scene navigation, controller re-init) do not
 * throw {@code "A bound value cannot be set"}.
 */
package com.damaitaliana.client.layout;
