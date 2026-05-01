package com.damaitaliana.client.ui.board.animation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.shared.domain.CaptureSequence;
import com.damaitaliana.shared.domain.SimpleMove;
import com.damaitaliana.shared.domain.Square;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.Test;

class AnimationOrchestratorTest {

  private static final double CELL = 80.0;

  @Test
  void simpleMoveProducesTranslateTransition() {
    SimpleMove move = new SimpleMove(new Square(2, 2), new Square(3, 3));
    Node piece = new Rectangle();

    Animation anim = AnimationOrchestrator.animateMove(move, sq -> piece, CELL);

    assertThat(anim).isInstanceOf(TranslateTransition.class);
    TranslateTransition tt = (TranslateTransition) anim;
    assertThat(tt.getNode()).isSameAs(piece);
    assertThat(tt.getByX()).isEqualTo(CELL);
    // rank 2 -> rank 3 is "upward" in the SPEC but "negative" in JavaFX y-axis
    assertThat(tt.getByY()).isEqualTo(-CELL);
  }

  @Test
  void simpleMoveDeltasReflectFileAndRankDifferences() {
    SimpleMove move = new SimpleMove(new Square(0, 7), new Square(2, 5));
    Node piece = new Rectangle();

    TranslateTransition tt =
        (TranslateTransition) AnimationOrchestrator.animateMove(move, sq -> piece, CELL);

    assertThat(tt.getByX()).isEqualTo(2 * CELL);
    assertThat(tt.getByY()).isEqualTo(2 * CELL); // rank 7 -> 5 is downward in JavaFX
  }

  @Test
  void singleCaptureProducesSequentialOfOneParallelLeg() {
    CaptureSequence cs =
        new CaptureSequence(new Square(2, 2), List.of(new Square(4, 4)), List.of(new Square(3, 3)));
    Node piece = new Rectangle();
    Node captured = new Rectangle();
    Map<Square, Node> nodes = new HashMap<>();
    nodes.put(new Square(2, 2), piece);
    nodes.put(new Square(3, 3), captured);

    Animation anim = AnimationOrchestrator.animateMove(cs, nodes::get, CELL);

    assertThat(anim).isInstanceOf(SequentialTransition.class);
    SequentialTransition seq = (SequentialTransition) anim;
    assertThat(seq.getChildren()).hasSize(1);
    assertThat(seq.getChildren().get(0)).isInstanceOf(ParallelTransition.class);

    ParallelTransition leg = (ParallelTransition) seq.getChildren().get(0);
    assertThat(leg.getChildren()).hasSize(2); // slide + fadeCapture
  }

  @Test
  void multiCaptureProducesOneLegPerJump() {
    CaptureSequence cs =
        new CaptureSequence(
            new Square(2, 2),
            List.of(new Square(4, 4), new Square(6, 2)),
            List.of(new Square(3, 3), new Square(5, 3)));
    Node piece = new Rectangle();
    Function<Square, Node> resolver = sq -> sq.equals(new Square(2, 2)) ? piece : new Rectangle();

    Animation anim = AnimationOrchestrator.animateMove(cs, resolver, CELL);

    SequentialTransition seq = (SequentialTransition) anim;
    assertThat(seq.getChildren()).hasSize(2);
    seq.getChildren().forEach(a -> assertThat(a).isInstanceOf(ParallelTransition.class));
  }

  @Test
  void capturedNodeMissingFromResolverDegradesToSlideOnlyLeg() {
    CaptureSequence cs =
        new CaptureSequence(new Square(2, 2), List.of(new Square(4, 4)), List.of(new Square(3, 3)));
    Node piece = new Rectangle();
    Function<Square, Node> resolver = sq -> sq.equals(new Square(2, 2)) ? piece : null;

    Animation anim = AnimationOrchestrator.animateMove(cs, resolver, CELL);

    SequentialTransition seq = (SequentialTransition) anim;
    assertThat(seq.getChildren().get(0)).isInstanceOf(TranslateTransition.class);
  }

  @Test
  void rejectsCellSizeNotPositive() {
    SimpleMove m = new SimpleMove(new Square(0, 0), new Square(1, 1));
    Function<Square, Node> resolver = sq -> new Rectangle();
    assertThatThrownBy(() -> AnimationOrchestrator.animateMove(m, resolver, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> AnimationOrchestrator.animateMove(m, resolver, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullArguments() {
    SimpleMove m = new SimpleMove(new Square(0, 0), new Square(1, 1));
    Function<Square, Node> resolver = sq -> new Rectangle();
    assertThatNullPointerException()
        .isThrownBy(() -> AnimationOrchestrator.animateMove(null, resolver, CELL));
    assertThatNullPointerException()
        .isThrownBy(() -> AnimationOrchestrator.animateMove(m, null, CELL));
  }

  @Test
  void captureLegWithParticleHostAddsSplashAsThirdChild() {
    // Task 3.5.8 — when a Pane host is provided, each capture leg becomes a 3-child
    // ParallelTransition: slide + fadeCapture + ParticleEffects.captureSplash.
    CaptureSequence cs =
        new CaptureSequence(new Square(2, 2), List.of(new Square(4, 4)), List.of(new Square(3, 3)));
    Node piece = new Rectangle();
    Node captured = new Rectangle();
    Map<Square, Node> nodes = new HashMap<>();
    nodes.put(new Square(2, 2), piece);
    nodes.put(new Square(3, 3), captured);
    Pane host = new Pane();

    Animation anim = AnimationOrchestrator.animateMove(cs, nodes::get, CELL, host);

    SequentialTransition seq = (SequentialTransition) anim;
    assertThat(seq.getChildren()).hasSize(1);
    ParallelTransition leg = (ParallelTransition) seq.getChildren().get(0);
    assertThat(leg.getChildren()).hasSize(3); // slide + fadeCapture + captureSplash
    assertThat(host.getChildren()).hasSize(ParticleEffects.SPLASH_PARTICLE_COUNT);
  }

  @Test
  void rejectsResolverThatReturnsNullForFromSquare() {
    SimpleMove m = new SimpleMove(new Square(0, 0), new Square(1, 1));
    Function<Square, Node> resolver = sq -> null;
    assertThatNullPointerException()
        .isThrownBy(() -> AnimationOrchestrator.animateMove(m, resolver, CELL));
  }
}
