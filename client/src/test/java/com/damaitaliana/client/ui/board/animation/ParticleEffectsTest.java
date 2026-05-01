package com.damaitaliana.client.ui.board.animation;

import static org.assertj.core.api.Assertions.assertThat;

import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.Test;

/**
 * Pure-Java assertions on {@link ParticleEffects} — JavaFX shapes / panes / timelines instantiate
 * without the toolkit, so these tests verify node attachment + animation structure without booting
 * JavaFX. Lifecycle of {@code setOnFinished} cleanup is exercised indirectly by checking that the
 * handler is set; full playback would require a running toolkit and is covered manually.
 */
class ParticleEffectsTest {

  @Test
  void captureSplashAttachesParticlesEagerlyAndReturnsParallelOfSameSize() {
    Pane host = new Pane();
    assertThat(host.getChildren()).isEmpty();

    Animation anim = ParticleEffects.captureSplash(host, 40, 40);

    assertThat(host.getChildren()).hasSize(ParticleEffects.SPLASH_PARTICLE_COUNT);
    assertThat(host.getChildren()).allMatch(n -> n instanceof Circle);
    assertThat(anim).isInstanceOf(ParallelTransition.class);
    assertThat(((ParallelTransition) anim).getChildren())
        .hasSize(ParticleEffects.SPLASH_PARTICLE_COUNT);
    assertThat(anim.getOnFinished()).isNotNull();
  }

  @Test
  void captureSplashParticlesStartInvisibleAndPositionedAtOrigin() {
    Pane host = new Pane();
    ParticleEffects.captureSplash(host, 60, 90);

    Circle first = (Circle) host.getChildren().get(0);
    assertThat(first.getOpacity()).isZero();
    assertThat(first.getTranslateX()).isEqualTo(60);
    assertThat(first.getTranslateY()).isEqualTo(90);
    assertThat(first.isMouseTransparent()).isTrue();
  }

  @Test
  void promotionGlowAttachesRaysAndReturnsParallelOfSameSize() {
    Pane host = new Pane();

    Animation anim = ParticleEffects.promotionGlow(host, 50, 50);

    assertThat(host.getChildren()).hasSize(ParticleEffects.PROMOTION_RAY_COUNT);
    assertThat(host.getChildren()).allMatch(n -> n instanceof Line);
    assertThat(anim).isInstanceOf(ParallelTransition.class);
    assertThat(((ParallelTransition) anim).getChildren())
        .hasSize(ParticleEffects.PROMOTION_RAY_COUNT);
    assertThat(anim.getTotalDuration()).isEqualTo(ParticleEffects.PROMOTION_GLOW_DURATION);
  }

  @Test
  void mandatoryGlowAppliesDropShadowAndReturnsIndefiniteTimeline() {
    Rectangle piece = new Rectangle(40, 40);
    assertThat(piece.getEffect()).isNull();

    Timeline timeline = ParticleEffects.mandatoryGlow(piece);

    assertThat(piece.getEffect()).isInstanceOf(DropShadow.class);
    assertThat(timeline.getCycleCount()).isEqualTo(Animation.INDEFINITE);
    assertThat(timeline.getKeyFrames()).hasSize(3);
    DropShadow halo = (DropShadow) piece.getEffect();
    assertThat(halo.getColor().toString()).isEqualTo("0xffd700ff");
  }

  @Test
  void splashAndGlowConstantsHaveExpectedValues() {
    assertThat(ParticleEffects.SPLASH_PARTICLE_COUNT).isEqualTo(10);
    assertThat(ParticleEffects.PROMOTION_RAY_COUNT).isEqualTo(10);
    assertThat(ParticleEffects.SPLASH_DURATION.toMillis()).isEqualTo(350);
    assertThat(ParticleEffects.PROMOTION_GLOW_DURATION.toMillis()).isEqualTo(600);
    assertThat(ParticleEffects.MANDATORY_GLOW_CYCLE.toMillis()).isEqualTo(1200);
  }
}
