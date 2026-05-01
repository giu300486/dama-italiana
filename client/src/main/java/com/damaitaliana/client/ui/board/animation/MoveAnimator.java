package com.damaitaliana.client.ui.board.animation;

import java.util.Objects;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Stateless factory of JavaFX {@link Animation} objects that match the SPEC §13.3 parameters.
 *
 * <p>Durations are exposed as {@code public static final} fields so callers and tests can refer to
 * the SPEC values without re-deriving them. Animations returned here are configured but not played;
 * callers are expected to compose / chain them and then call {@code playFromStart}.
 */
public final class MoveAnimator {

  /** SPEC §13.3 — TranslateTransition 250 ms with out-back overshoot for piece moves. */
  public static final Duration MOVE_DURATION = Duration.millis(250);

  /** SPEC §13.3 — ScaleTransition 200 ms scale-out + FadeTransition parallela for captures. */
  public static final Duration CAPTURE_DURATION = Duration.millis(200);

  /** SPEC §13.3 — Flash dorato + RotateTransition 500 ms for promotion. */
  public static final Duration PROMOTION_DURATION = Duration.millis(500);

  /** SPEC §13.3 — pulsazione 800 ms cyclic for mandatory-capture highlight. */
  public static final Duration PULSE_DURATION = Duration.millis(800);

  /**
   * Out-back overshoot easing (SPEC §13.3, Task 3.5.8). The standard easings.net {@code
   * easeOutBack} curve: the piece accelerates, slightly overshoots its landing square (peak at
   * ~110%), then settles back — a juicy "weight" feedback compared to plain ease-out.
   *
   * <p>Implemented as a custom {@link Interpolator} subclass because JavaFX's {@link
   * Interpolator#SPLINE(double, double, double, double)} validates all four control-point
   * coordinates lie in [0,1], which forbids the overshoot {@code y1 > 1} a cubic-bezier
   * representation of out-back would need.
   */
  public static final Interpolator MOVE_INTERPOLATOR =
      new Interpolator() {
        @Override
        protected double curve(double t) {
          // easings.net easeOutBack: 1 + c3*(t-1)^3 + c1*(t-1)^2  with c1=1.70158, c3=c1+1.
          final double c1 = 1.70158;
          final double c3 = c1 + 1.0;
          double tm1 = t - 1.0;
          return 1.0 + c3 * tm1 * tm1 * tm1 + c1 * tm1 * tm1;
        }
      };

  /** Cell-relative opacity at the trough of the mandatory pulse. */
  public static final double PULSE_MIN_OPACITY = 0.4;

  private MoveAnimator() {}

  /** Translates {@code node} by {@code (dx, dy)} pixels over {@link #MOVE_DURATION}. */
  public static TranslateTransition slideMove(Node node, double dx, double dy) {
    Objects.requireNonNull(node, "node");
    TranslateTransition tt = new TranslateTransition(MOVE_DURATION, node);
    tt.setByX(dx);
    tt.setByY(dy);
    tt.setInterpolator(MOVE_INTERPOLATOR);
    return tt;
  }

  /** Shrinks and fades {@code node} in parallel for capture removal. */
  public static ParallelTransition fadeCapture(Node node) {
    Objects.requireNonNull(node, "node");
    ScaleTransition scale = new ScaleTransition(CAPTURE_DURATION, node);
    scale.setToX(0);
    scale.setToY(0);
    FadeTransition fade = new FadeTransition(CAPTURE_DURATION, node);
    fade.setFromValue(1.0);
    fade.setToValue(0.0);
    return new ParallelTransition(scale, fade);
  }

  /**
   * Rotates {@code piece} 360° on the Y axis ({@link #PROMOTION_DURATION}) while a brief golden
   * flash overlays the cell. The flash fades in then out across the same duration.
   */
  public static ParallelTransition promotion(Node piece, Node flashOverlay) {
    Objects.requireNonNull(piece, "piece");
    Objects.requireNonNull(flashOverlay, "flashOverlay");

    RotateTransition rotate = new RotateTransition(PROMOTION_DURATION, piece);
    rotate.setAxis(new Point3D(0, 1, 0));
    rotate.setByAngle(360);

    Duration half = PROMOTION_DURATION.divide(2);
    FadeTransition flashIn = new FadeTransition(half, flashOverlay);
    flashIn.setFromValue(0.0);
    flashIn.setToValue(1.0);
    FadeTransition flashOut = new FadeTransition(half, flashOverlay);
    flashOut.setFromValue(1.0);
    flashOut.setToValue(0.0);
    SequentialTransition flash = new SequentialTransition(flashIn, flashOut);

    return new ParallelTransition(rotate, flash);
  }

  /**
   * Cyclic auto-reversing fade between full opacity and {@link #PULSE_MIN_OPACITY}. Used for the
   * mandatory-capture highlight (SPEC §13.3 — implemented with {@link FadeTransition} rather than
   * {@link javafx.animation.FillTransition} since {@link javafx.scene.layout.Region} cells have no
   * {@code fill} property; see {@code package-info}).
   */
  public static FadeTransition pulseMandatory(Node node) {
    Objects.requireNonNull(node, "node");
    FadeTransition pulse = new FadeTransition(PULSE_DURATION, node);
    pulse.setFromValue(1.0);
    pulse.setToValue(PULSE_MIN_OPACITY);
    pulse.setCycleCount(Animation.INDEFINITE);
    pulse.setAutoReverse(true);
    return pulse;
  }
}
