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
 * </ul>
 */
public final class BoardFrameThicknessHelper {

  static final double THICKNESS_MIN_PX = 16.0;
  static final double THICKNESS_MAX_PX = 48.0;
  static final double THICKNESS_SCALE = 0.035;

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
                  double boardSide = Math.min(renderer.getWidth(), renderer.getHeight());
                  double t =
                      computeFrameThickness(
                          boardSide, THICKNESS_MIN_PX, THICKNESS_MAX_PX, THICKNESS_SCALE);
                  return new Insets(t);
                },
                renderer.widthProperty(),
                renderer.heightProperty()));
  }
}
