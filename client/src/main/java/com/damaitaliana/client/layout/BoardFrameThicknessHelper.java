package com.damaitaliana.client.layout;

import com.damaitaliana.client.ui.board.BoardRenderer;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;

/**
 * Programmatic proportional thickness for the wood frame surrounding the {@link BoardRenderer}
 * (F4.5 Task 4.5.8 + ADR-043). The {@code .board-frame} StackPane in {@code board-view.fxml} used
 * to declare a fixed 24 px padding; at 4K the frame visually disappeared, while at the 1024 × 720
 * floor it dominated the available area.
 *
 * <p>Formula: {@code thickness = clamp(minPx, maxPx, boardSide × scaleFactor)} where {@code
 * boardSide = min(rendererWidth, rendererHeight)} (cells are square so the playing area is always a
 * square inscribed in the renderer's available rectangle, see {@link BoardRenderer#layoutChildren
 * layoutChildren}).
 *
 * <ul>
 *   <li>{@link #THICKNESS_MIN_PX} = 16 — frame stays visible at the 1024 × 720 floor.
 *   <li>{@link #THICKNESS_MAX_PX} = 48 — frame caps before dominating the playing area at 4K.
 *   <li>{@link #THICKNESS_SCALE} = 0.035 — 3.5 % of board side, fluid in the middle range (board ~
 *       460–1370 px maps linearly to thickness 16–48).
 *   <li>{@link #PRE_LAYOUT_FALLBACK_PX} = 24 — used until the renderer has been laid out (width or
 *       height still 0). Matches the static FXML padding so the binding does not visibly shrink the
 *       frame from 24 to 16 between FXML load and the first layout pulse (F4.5 REVIEW F-005).
 * </ul>
 */
public final class BoardFrameThicknessHelper {

  static final double THICKNESS_MIN_PX = 16.0;
  static final double THICKNESS_MAX_PX = 48.0;
  static final double THICKNESS_SCALE = 0.035;

  /**
   * F4.5 REVIEW F-005 — padding used by {@link #bindFrameThickness} until the renderer has been
   * sized (i.e. {@code width > 0} and {@code height > 0}). Mirrors the static {@code 24 px} padding
   * declared in {@code board-view.fxml} so users do not see the frame snap from 24 to 16 between
   * the controller's {@code initialize()} call and the first layout pulse.
   */
  static final double PRE_LAYOUT_FALLBACK_PX = 24.0;

  private BoardFrameThicknessHelper() {}

  /** Pure math, exposed for unit tests. */
  public static double computeFrameThickness(
      double boardSide, double minPx, double maxPx, double scaleFactor) {
    return Math.min(maxPx, Math.max(minPx, boardSide * scaleFactor));
  }

  /**
   * Binds the {@code paddingProperty} of {@code frame} to a {@link Insets} computed from {@code
   * min(renderer.width, renderer.height) × scaleFactor} clamped to {@code [minPx, maxPx]}. The
   * binding tracks both width and height of the renderer so the frame thickness reacts to live
   * window resize. Idempotent: re-binds existing bindings cleanly.
   *
   * <p>F4.5 REVIEW F-005: while the renderer has not been laid out yet ({@code width == 0} or
   * {@code height == 0}), the binding emits {@link #PRE_LAYOUT_FALLBACK_PX} (24 px) instead of the
   * clamp floor (16 px) so the frame stays visually identical to the static FXML padding until the
   * first layout pulse fires.
   */
  public static void bindFrameThickness(StackPane frame, BoardRenderer renderer) {
    Objects.requireNonNull(frame, "frame");
    Objects.requireNonNull(renderer, "renderer");
    if (frame.paddingProperty().isBound()) {
      frame.paddingProperty().unbind();
    }
    frame
        .paddingProperty()
        .bind(
            Bindings.createObjectBinding(
                () -> {
                  double w = renderer.getWidth();
                  double h = renderer.getHeight();
                  if (w <= 0 || h <= 0) {
                    return new Insets(PRE_LAYOUT_FALLBACK_PX);
                  }
                  double boardSide = Math.min(w, h);
                  double t =
                      computeFrameThickness(
                          boardSide, THICKNESS_MIN_PX, THICKNESS_MAX_PX, THICKNESS_SCALE);
                  return new Insets(t);
                },
                renderer.widthProperty(),
                renderer.heightProperty()));
  }
}
