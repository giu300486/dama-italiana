package com.damaitaliana.client.layout;

import java.util.Locale;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;

/**
 * Programmatic fluid typography for the F4.5 responsive baseline (PLAN-fase-4.5 Task 4.5.7 +
 * ADR-043). JavaFX 21 CSS does not support a {@code clamp(min, vw, max)} font-size like web CSS
 * does — labels carrying the {@code display-fluid} or {@code display-fluid-lg} marker classes get
 * their {@code -fx-font-size} bound at runtime to the scene width via {@link
 * #applyFluidFontsToScene(Scene)}.
 *
 * <p>Formula: {@code fontSize = clamp(minPx, maxPx, sceneWidth × scaleFactor)}.
 *
 * <ul>
 *   <li>{@code display-fluid} (medium): min=24, max=32, scale=0.018 — at 1024 → 24, at 1778 → 32.
 *   <li>{@code display-fluid-lg} (hero): min=28, max=48, scale=0.025 — PLAN-specified, at 1024 →
 *       28, at 1920 → 48.
 * </ul>
 *
 * <p>The CSS classes still declare a baseline {@code -fx-font-size} so labels look right when no
 * scene is attached (unit tests, FXML preview tools).
 */
public final class JavaFxScalingHelper {

  /** {@code display-fluid} — section title fluid range. */
  static final double FLUID_MIN_PX = 24.0;

  static final double FLUID_MAX_PX = 32.0;
  static final double FLUID_SCALE = 0.018;

  /** {@code display-fluid-lg} — hero / display title fluid range (PLAN §4.5.7 spec). */
  static final double FLUID_LG_MIN_PX = 28.0;

  static final double FLUID_LG_MAX_PX = 48.0;
  static final double FLUID_LG_SCALE = 0.025;

  static final String STYLE_CLASS_FLUID = "display-fluid";
  static final String STYLE_CLASS_FLUID_LG = "display-fluid-lg";

  private JavaFxScalingHelper() {}

  /** Pure math, exposed for unit tests. */
  public static double computeFluidFontSize(
      double sceneWidth, double minPx, double maxPx, double scaleFactor) {
    return Math.min(maxPx, Math.max(minPx, sceneWidth * scaleFactor));
  }

  /**
   * Walks the {@code scene.getRoot()} tree and binds the {@code styleProperty} of every {@link
   * Labeled} carrying the {@code display-fluid} or {@code display-fluid-lg} marker class to a
   * {@code -fx-font-size} computed from the scene width. Idempotent: re-binds existing bindings
   * cleanly (unbind first), so the {@link com.damaitaliana.client.app.SceneRouter SceneRouter} can
   * call this on every navigation without leaks.
   */
  public static void applyFluidFontsToScene(Scene scene) {
    if (scene == null || scene.getRoot() == null) {
      return;
    }
    walkAndBind(scene.getRoot(), scene.widthProperty());
  }

  private static void walkAndBind(Node node, ObservableDoubleValue sceneWidth) {
    if (node instanceof Labeled labeled) {
      if (labeled.getStyleClass().contains(STYLE_CLASS_FLUID_LG)) {
        bindFluidFontSize(labeled, sceneWidth, FLUID_LG_MIN_PX, FLUID_LG_MAX_PX, FLUID_LG_SCALE);
      } else if (labeled.getStyleClass().contains(STYLE_CLASS_FLUID)) {
        bindFluidFontSize(labeled, sceneWidth, FLUID_MIN_PX, FLUID_MAX_PX, FLUID_SCALE);
      }
    }
    if (node instanceof Parent parent) {
      for (Node child : parent.getChildrenUnmodifiable()) {
        walkAndBind(child, sceneWidth);
      }
    }
  }

  /**
   * Visible for tests. Binds a single label's font-size to {@code sceneWidth × scaleFactor} clamped
   * to {@code [minPx, maxPx]}.
   */
  static void bindFluidFontSize(
      Labeled label,
      ObservableDoubleValue sceneWidth,
      double minPx,
      double maxPx,
      double scaleFactor) {
    Objects.requireNonNull(label, "label");
    Objects.requireNonNull(sceneWidth, "sceneWidth");
    if (label.styleProperty().isBound()) {
      label.styleProperty().unbind();
    }
    label
        .styleProperty()
        .bind(
            Bindings.createStringBinding(
                () ->
                    String.format(
                        Locale.ROOT,
                        "-fx-font-size: %.2fpx;",
                        computeFluidFontSize(sceneWidth.get(), minPx, maxPx, scaleFactor)),
                sceneWidth));
  }
}
