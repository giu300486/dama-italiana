package com.damaitaliana.client.ui.board.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;

/**
 * Juicy particle and glow effects (Task 3.5.8) layered on top of the {@link MoveAnimator}
 * primitives. All factory methods are stateless static helpers; they create the visual nodes,
 * attach them to the supplied host {@link Pane} (eagerly, so unit tests can assert their presence
 * without spinning up the JavaFX toolkit), build the per-particle animations, and configure {@code
 * setOnFinished} to remove the nodes when the animation completes.
 *
 * <p>Particles start at {@code opacity = 0} so they are invisible during the brief window between
 * orchestrator-build and {@code playFromStart}; the per-particle {@link FadeTransition} fades them
 * in at the splash peak before fading out.
 */
public final class ParticleEffects {

  /** Number of particles per capture splash. */
  public static final int SPLASH_PARTICLE_COUNT = 10;

  /** Number of rays per promotion glow. */
  public static final int PROMOTION_RAY_COUNT = 10;

  /** Capture splash total duration. */
  public static final Duration SPLASH_DURATION = Duration.millis(350);

  /** Promotion glow total duration. */
  public static final Duration PROMOTION_GLOW_DURATION = Duration.millis(600);

  /** Mandatory-capture halo cycle duration (one full pulse). */
  public static final Duration MANDATORY_GLOW_CYCLE = Duration.millis(1200);

  private static final double SPLASH_RADIUS_PX = 28.0;
  private static final double SPLASH_PARTICLE_RADIUS_PX = 3.0;
  private static final Color[] SPLASH_COLORS = {
    Color.web("#6B4423"), // warm brown
    Color.web("#8C6A4A"), // mid brown
    Color.web("#9E9285"), // dust grey
  };

  private static final double PROMOTION_RAY_LENGTH_PX = 36.0;
  private static final Color PROMOTION_RAY_INNER = Color.web("#FFD700");
  private static final Color PROMOTION_RAY_OUTER = Color.web("#FFA500");

  private static final double MANDATORY_GLOW_RADIUS_MIN = 12.0;
  private static final double MANDATORY_GLOW_RADIUS_MAX = 24.0;
  private static final Color MANDATORY_GLOW_COLOR = Color.web("#FFD700");

  private ParticleEffects() {}

  /**
   * Eight-to-ten dust-coloured circles burst radially from {@code (x, y)} on the {@code host} pane,
   * each fading and shrinking over {@link #SPLASH_DURATION}. Returns the parallel animation; when
   * it finishes, the spawned nodes are removed from the host.
   */
  public static Animation captureSplash(Pane host, double x, double y) {
    Objects.requireNonNull(host, "host");
    List<Circle> particles = new ArrayList<>();
    List<Animation> perParticle = new ArrayList<>();
    for (int i = 0; i < SPLASH_PARTICLE_COUNT; i++) {
      Circle p = new Circle(SPLASH_PARTICLE_RADIUS_PX, SPLASH_COLORS[i % SPLASH_COLORS.length]);
      p.setTranslateX(x);
      p.setTranslateY(y);
      p.setOpacity(0); // invisible until the fade-in keyframe runs
      p.setMouseTransparent(true);
      particles.add(p);
      host.getChildren().add(p);

      double angle = 2 * Math.PI * i / SPLASH_PARTICLE_COUNT;
      double dx = Math.cos(angle) * SPLASH_RADIUS_PX;
      double dy = Math.sin(angle) * SPLASH_RADIUS_PX;

      TranslateTransition tx = new TranslateTransition(SPLASH_DURATION, p);
      tx.setByX(dx);
      tx.setByY(dy);

      FadeTransition fade = new FadeTransition(SPLASH_DURATION, p);
      fade.setFromValue(0.85);
      fade.setToValue(0);

      ScaleTransition shrink = new ScaleTransition(SPLASH_DURATION, p);
      shrink.setFromX(1.0);
      shrink.setFromY(1.0);
      shrink.setToX(0.4);
      shrink.setToY(0.4);

      perParticle.add(new ParallelTransition(tx, fade, shrink));
    }
    ParallelTransition burst = new ParallelTransition(perParticle.toArray(Animation[]::new));
    burst.setOnFinished(e -> host.getChildren().removeAll(particles));
    return burst;
  }

  /**
   * Eight-to-ten golden lines explode radially from {@code (x, y)}, scaling out and fading over
   * {@link #PROMOTION_GLOW_DURATION}. Layered on top of the existing flash overlay built by {@link
   * MoveAnimator#promotion(Node, Node)}.
   */
  public static Animation promotionGlow(Pane host, double x, double y) {
    Objects.requireNonNull(host, "host");
    List<Line> rays = new ArrayList<>();
    List<Animation> perRay = new ArrayList<>();
    for (int i = 0; i < PROMOTION_RAY_COUNT; i++) {
      double angle = 2 * Math.PI * i / PROMOTION_RAY_COUNT;
      double endX = Math.cos(angle) * PROMOTION_RAY_LENGTH_PX;
      double endY = Math.sin(angle) * PROMOTION_RAY_LENGTH_PX;
      Line ray = new Line(0, 0, endX, endY);
      ray.setStroke(i % 2 == 0 ? PROMOTION_RAY_INNER : PROMOTION_RAY_OUTER);
      ray.setStrokeWidth(2.0);
      ray.setTranslateX(x);
      ray.setTranslateY(y);
      ray.setOpacity(0);
      ray.setMouseTransparent(true);
      ray.setScaleX(0.2);
      ray.setScaleY(0.2);
      rays.add(ray);
      host.getChildren().add(ray);

      ScaleTransition expand = new ScaleTransition(PROMOTION_GLOW_DURATION, ray);
      expand.setFromX(0.2);
      expand.setFromY(0.2);
      expand.setToX(1.0);
      expand.setToY(1.0);

      FadeTransition fade = new FadeTransition(PROMOTION_GLOW_DURATION, ray);
      fade.setFromValue(0.95);
      fade.setToValue(0);

      perRay.add(new ParallelTransition(expand, fade));
    }
    ParallelTransition burst = new ParallelTransition(perRay.toArray(Animation[]::new));
    burst.setOnFinished(e -> host.getChildren().removeAll(rays));
    return burst;
  }

  /**
   * Indefinite gold-halo pulse applied to {@code piece}. The {@link DropShadow} effect is set on
   * the piece and animated by a cyclic {@link Timeline} that interpolates its radius between {@link
   * #MANDATORY_GLOW_RADIUS_MIN} and {@link #MANDATORY_GLOW_RADIUS_MAX}. Stop the returned timeline
   * to clear the highlight; the caller is responsible for removing the {@link DropShadow} effect
   * from the piece if desired (the timeline's {@code setOnFinished} clears it on natural stop).
   */
  public static Timeline mandatoryGlow(Node piece) {
    Objects.requireNonNull(piece, "piece");
    DropShadow halo = new DropShadow();
    halo.setColor(MANDATORY_GLOW_COLOR);
    halo.setRadius(MANDATORY_GLOW_RADIUS_MIN);
    halo.setSpread(0.45);
    piece.setEffect(halo);

    Timeline timeline = new Timeline();
    timeline
        .getKeyFrames()
        .addAll(
            new KeyFrame(
                Duration.ZERO, new KeyValue(halo.radiusProperty(), MANDATORY_GLOW_RADIUS_MIN)),
            new KeyFrame(
                MANDATORY_GLOW_CYCLE.divide(2),
                new KeyValue(halo.radiusProperty(), MANDATORY_GLOW_RADIUS_MAX)),
            new KeyFrame(
                MANDATORY_GLOW_CYCLE,
                new KeyValue(halo.radiusProperty(), MANDATORY_GLOW_RADIUS_MIN)));
    timeline.setCycleCount(Animation.INDEFINITE);
    timeline.setOnFinished(e -> piece.setEffect(null));
    return timeline;
  }
}
