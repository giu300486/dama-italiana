package com.damaitaliana.client.ui.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.client.persistence.SerializedGameState;
import com.damaitaliana.client.ui.board.animation.MoveAnimator;
import com.damaitaliana.shared.domain.CaptureSequence;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.SimpleMove;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.notation.FidNotation;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import javafx.animation.Animation;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RulesAnimationsTest {

  private static boolean fxToolkitReady;

  private final RulesAnimations animations = new RulesAnimations();

  @BeforeAll
  static void initToolkit() {
    try {
      Platform.startup(() -> {});
      fxToolkitReady = true;
    } catch (IllegalStateException alreadyStarted) {
      fxToolkitReady = true;
    } catch (UnsupportedOperationException headless) {
      fxToolkitReady = false;
    }
  }

  @Test
  void simpleCaptureDescribesAManJumpingAnAdjacentManForward() {
    SerializedGameState start = animations.startingPosition(RulesAnimations.Kind.SIMPLE_CAPTURE);
    assertThat(start.whiteMen()).containsExactly(22);
    assertThat(start.blackMen()).containsExactly(18);

    Move move = animations.move(RulesAnimations.Kind.SIMPLE_CAPTURE);
    assertThat(move).isInstanceOf(CaptureSequence.class);
    CaptureSequence cs = (CaptureSequence) move;
    assertThat(cs.captureCount()).isEqualTo(1);
    assertThat(cs.from()).isEqualTo(FidNotation.toSquare(22));
    assertThat(cs.path()).containsExactly(FidNotation.toSquare(15));
    assertThat(cs.captured()).containsExactly(FidNotation.toSquare(18));
  }

  @Test
  void multiCaptureChainsTwoJumpsThroughForwardDiagonals() {
    SerializedGameState start = animations.startingPosition(RulesAnimations.Kind.MULTI_CAPTURE);
    assertThat(start.whiteMen()).containsExactly(26);
    assertThat(start.blackMen()).containsExactly(22, 14);

    Move move = animations.move(RulesAnimations.Kind.MULTI_CAPTURE);
    assertThat(move).isInstanceOf(CaptureSequence.class);
    CaptureSequence cs = (CaptureSequence) move;
    assertThat(cs.captureCount()).isEqualTo(2);
    assertThat(cs.from()).isEqualTo(FidNotation.toSquare(26));
    assertThat(cs.path()).containsExactly(FidNotation.toSquare(17), FidNotation.toSquare(10));
    assertThat(cs.captured()).containsExactly(FidNotation.toSquare(22), FidNotation.toSquare(14));
  }

  @Test
  void promotionMovesAManToTheLastRow() {
    SerializedGameState start = animations.startingPosition(RulesAnimations.Kind.PROMOTION);
    assertThat(start.whiteMen()).containsExactly(5);
    assertThat(start.blackMen()).isEmpty();

    Move move = animations.move(RulesAnimations.Kind.PROMOTION);
    assertThat(move).isInstanceOf(SimpleMove.class);
    SimpleMove sm = (SimpleMove) move;
    assertThat(sm.from()).isEqualTo(FidNotation.toSquare(5));
    assertThat(sm.to()).isEqualTo(FidNotation.toSquare(1));
    assertThat(sm.to().rank()).isEqualTo(7);
  }

  @Test
  void captionKeysAreUniqueAndScoped() {
    String simple = animations.captionKey(RulesAnimations.Kind.SIMPLE_CAPTURE);
    String multi = animations.captionKey(RulesAnimations.Kind.MULTI_CAPTURE);
    String promo = animations.captionKey(RulesAnimations.Kind.PROMOTION);
    assertThat(simple).startsWith("rules.animation.").isNotEqualTo(multi).isNotEqualTo(promo);
    assertThat(multi).startsWith("rules.animation.").isNotEqualTo(promo);
  }

  @Test
  void animationCellSizeMustBePositive() {
    Function<Square, Node> resolver = sq -> new Rectangle(10, 10);
    assertThatThrownBy(() -> animations.animation(RulesAnimations.Kind.SIMPLE_CAPTURE, resolver, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void simpleCaptureBuildsAnAnimationThatPlaysWithoutErrors() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    Animation anim =
        runOnFxThread(
            () ->
                animations.animation(
                    RulesAnimations.Kind.SIMPLE_CAPTURE, sq -> new Rectangle(10, 10), 32.0));

    assertThat(anim).isNotNull();
    CountDownLatch finished = new CountDownLatch(1);
    runOnFxThread(
        () -> {
          anim.setOnFinished(e -> finished.countDown());
          anim.setRate(50.0); // speed up to keep the test fast
          anim.playFromStart();
          return null;
        });
    assertThat(finished.await(5, TimeUnit.SECONDS)).as("animation completed").isTrue();
  }

  @Test
  void multipleCapturePlaysSequentiallyWithOneLegPerJump() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    Animation anim =
        runOnFxThread(
            () ->
                animations.animation(
                    RulesAnimations.Kind.MULTI_CAPTURE, sq -> new Rectangle(10, 10), 32.0));

    assertThat(anim).isInstanceOf(SequentialTransition.class);
    SequentialTransition seq = (SequentialTransition) anim;
    assertThat(seq.getChildren()).hasSize(2);
  }

  @Test
  void promotionAnimationHas500msPromotionPhase() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    Animation anim =
        runOnFxThread(
            () ->
                animations.animation(
                    RulesAnimations.Kind.PROMOTION, sq -> new Rectangle(10, 10), 32.0));

    assertThat(anim).isInstanceOf(SequentialTransition.class);
    SequentialTransition seq = (SequentialTransition) anim;
    assertThat(seq.getChildren()).hasSize(2);
    Animation promo = seq.getChildren().get(1);
    assertThat(promo.getTotalDuration()).isEqualTo(MoveAnimator.PROMOTION_DURATION);
    assertThat(MoveAnimator.PROMOTION_DURATION).isEqualTo(Duration.millis(500));
  }

  private static <T> T runOnFxThread(java.util.concurrent.Callable<T> task) throws Exception {
    AtomicReference<T> holder = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          try {
            holder.set(task.call());
          } catch (Throwable t) {
            failure.set(t);
          } finally {
            latch.countDown();
          }
        });
    if (!latch.await(10, TimeUnit.SECONDS)) {
      throw new IllegalStateException("FX task did not complete within 10s");
    }
    if (failure.get() != null) {
      throw new RuntimeException(failure.get());
    }
    return holder.get();
  }
}
