package com.damaitaliana.client.ui.board.animation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

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
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.Test;

class MoveAnimatorTest {

  private final Node node = new Rectangle();
  private final Node overlay = new Rectangle();

  @Test
  void slideMoveProducesTransitionWithExpectedDurationAndInterpolator() {
    TranslateTransition tt = MoveAnimator.slideMove(node, 80, -80);

    assertThat(tt.getDuration()).isEqualTo(MoveAnimator.MOVE_DURATION);
    assertThat(tt.getInterpolator()).isEqualTo(MoveAnimator.MOVE_INTERPOLATOR);
    assertThat(tt.getNode()).isSameAs(node);
    assertThat(tt.getByX()).isEqualTo(80);
    assertThat(tt.getByY()).isEqualTo(-80);
  }

  @Test
  void slideMoveInterpolatorIsEaseOut() {
    TranslateTransition tt = MoveAnimator.slideMove(node, 0, 0);
    assertThat(tt.getInterpolator()).isSameAs(Interpolator.EASE_OUT);
  }

  @Test
  void fadeCaptureProducesParallelOfScaleAndFade() {
    ParallelTransition pt = MoveAnimator.fadeCapture(node);

    assertThat(pt.getChildren()).hasSize(2);
    ScaleTransition scale =
        pt.getChildren().stream()
            .filter(ScaleTransition.class::isInstance)
            .map(ScaleTransition.class::cast)
            .findFirst()
            .orElseThrow();
    FadeTransition fade =
        pt.getChildren().stream()
            .filter(FadeTransition.class::isInstance)
            .map(FadeTransition.class::cast)
            .findFirst()
            .orElseThrow();

    assertThat(scale.getDuration()).isEqualTo(MoveAnimator.CAPTURE_DURATION);
    assertThat(scale.getToX()).isEqualTo(0);
    assertThat(scale.getToY()).isEqualTo(0);
    assertThat(fade.getDuration()).isEqualTo(MoveAnimator.CAPTURE_DURATION);
    assertThat(fade.getToValue()).isEqualTo(0);
  }

  @Test
  void promotionAnimationIncludesRotateAndFlash() {
    ParallelTransition pt = MoveAnimator.promotion(node, overlay);

    assertThat(pt.getChildren()).hasSize(2);
    RotateTransition rotate =
        pt.getChildren().stream()
            .filter(RotateTransition.class::isInstance)
            .map(RotateTransition.class::cast)
            .findFirst()
            .orElseThrow();
    SequentialTransition flash =
        pt.getChildren().stream()
            .filter(SequentialTransition.class::isInstance)
            .map(SequentialTransition.class::cast)
            .findFirst()
            .orElseThrow();

    assertThat(rotate.getDuration()).isEqualTo(MoveAnimator.PROMOTION_DURATION);
    assertThat(rotate.getAxis()).isEqualTo(new Point3D(0, 1, 0));
    assertThat(rotate.getByAngle()).isEqualTo(360);
    assertThat(flash.getChildren()).hasSize(2);
    Animation flashIn = flash.getChildren().get(0);
    Animation flashOut = flash.getChildren().get(1);
    assertThat(flashIn).isInstanceOf(FadeTransition.class);
    assertThat(flashOut).isInstanceOf(FadeTransition.class);
  }

  @Test
  void pulseMandatoryIsCyclicAutoreverse() {
    FadeTransition pulse = MoveAnimator.pulseMandatory(node);

    assertThat(pulse.getDuration()).isEqualTo(MoveAnimator.PULSE_DURATION);
    assertThat(pulse.getCycleCount()).isEqualTo(Animation.INDEFINITE);
    assertThat(pulse.isAutoReverse()).isTrue();
    assertThat(pulse.getFromValue()).isEqualTo(1.0);
    assertThat(pulse.getToValue()).isEqualTo(MoveAnimator.PULSE_MIN_OPACITY);
  }

  @Test
  void specDurationsMatchSection13_3() {
    // Sanity: SPEC §13.3 nominal values.
    assertThat(MoveAnimator.MOVE_DURATION.toMillis()).isEqualTo(250);
    assertThat(MoveAnimator.CAPTURE_DURATION.toMillis()).isEqualTo(200);
    assertThat(MoveAnimator.PROMOTION_DURATION.toMillis()).isEqualTo(500);
    assertThat(MoveAnimator.PULSE_DURATION.toMillis()).isEqualTo(800);
  }

  @Test
  void rejectsNullNodes() {
    assertThatNullPointerException().isThrownBy(() -> MoveAnimator.slideMove(null, 0, 0));
    assertThatNullPointerException().isThrownBy(() -> MoveAnimator.fadeCapture(null));
    assertThatNullPointerException().isThrownBy(() -> MoveAnimator.promotion(null, overlay));
    assertThatNullPointerException().isThrownBy(() -> MoveAnimator.promotion(node, null));
    assertThatNullPointerException().isThrownBy(() -> MoveAnimator.pulseMandatory(null));
  }
}
