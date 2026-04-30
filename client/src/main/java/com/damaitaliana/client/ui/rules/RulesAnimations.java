package com.damaitaliana.client.ui.rules;

import com.damaitaliana.client.persistence.SerializedGameState;
import com.damaitaliana.client.ui.board.animation.AnimationOrchestrator;
import com.damaitaliana.client.ui.board.animation.MoveAnimator;
import com.damaitaliana.shared.domain.CaptureSequence;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.SimpleMove;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.notation.FidNotation;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import org.springframework.stereotype.Component;

/**
 * Static catalogue of demonstrative animations for the rules screen (FR-RUL-04, optional in Fase
 * 3). Three kinds are exposed — simple capture, multi-capture, promotion — each with a starting
 * position, the move to visualise and a localized caption key.
 *
 * <p>Animations re-use the production {@link AnimationOrchestrator}/{@link MoveAnimator} so the
 * rules screen and the live board view share the same visual vocabulary (SPEC §13.3 timings).
 */
@Component
public class RulesAnimations {

  /** The three demonstrative animation kinds. */
  public enum Kind {
    SIMPLE_CAPTURE,
    MULTI_CAPTURE,
    PROMOTION
  }

  /** Returns the position to display before the animation plays. */
  public SerializedGameState startingPosition(Kind kind) {
    Objects.requireNonNull(kind, "kind");
    return switch (kind) {
      case SIMPLE_CAPTURE ->
          new SerializedGameState(
              List.of(22), List.of(), List.of(18), List.of(), Color.WHITE, 0, List.of());
      case MULTI_CAPTURE ->
          new SerializedGameState(
              List.of(26), List.of(), List.of(22, 14), List.of(), Color.WHITE, 0, List.of());
      case PROMOTION ->
          new SerializedGameState(
              List.of(5), List.of(), List.of(), List.of(), Color.WHITE, 0, List.of());
    };
  }

  /** Returns the move that the animation visualises (always legal in the starting position). */
  public Move move(Kind kind) {
    Objects.requireNonNull(kind, "kind");
    return switch (kind) {
      case SIMPLE_CAPTURE ->
          new CaptureSequence(square(22), List.of(square(15)), List.of(square(18)));
      case MULTI_CAPTURE ->
          new CaptureSequence(
              square(26), List.of(square(17), square(10)), List.of(square(22), square(14)));
      case PROMOTION -> new SimpleMove(square(5), square(1));
    };
  }

  /** i18n key for the caption shown beneath the animation. */
  public String captionKey(Kind kind) {
    Objects.requireNonNull(kind, "kind");
    return switch (kind) {
      case SIMPLE_CAPTURE -> "rules.animation.simple_capture.caption";
      case MULTI_CAPTURE -> "rules.animation.multi_capture.caption";
      case PROMOTION -> "rules.animation.promotion.caption";
    };
  }

  /**
   * Builds the JavaFX {@link Animation} for {@code kind}. The {@code pieceAt} resolver and {@code
   * cellSize} match the live {@link com.damaitaliana.client.ui.board.BoardRenderer BoardRenderer}
   * showing the starting position. For {@link Kind#PROMOTION} the slide is followed by the {@link
   * MoveAnimator#promotion} effect (rotate + golden flash, 500 ms — SPEC §13.3 NFR-U-04).
   *
   * @throws NullPointerException if any argument is null.
   * @throws IllegalArgumentException if {@code cellSize} is non-positive.
   */
  public Animation animation(Kind kind, Function<Square, Node> pieceAt, double cellSize) {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(pieceAt, "pieceAt");
    if (cellSize <= 0) {
      throw new IllegalArgumentException("cellSize must be positive: " + cellSize);
    }
    Move move = move(kind);
    Animation moveAnimation = AnimationOrchestrator.animateMove(move, pieceAt, cellSize);
    if (kind != Kind.PROMOTION) {
      return moveAnimation;
    }
    Node piece = pieceAt.apply(move.from());
    Objects.requireNonNull(piece, "promotion: no piece node at " + move.from());
    Rectangle flashOverlay =
        new Rectangle(cellSize, cellSize, javafx.scene.paint.Color.web("#FFD700"));
    flashOverlay.setOpacity(0);
    flashOverlay.setMouseTransparent(true);
    ParallelTransition promo = MoveAnimator.promotion(piece, flashOverlay);
    return new SequentialTransition(moveAnimation, promo);
  }

  private static Square square(int fid) {
    return FidNotation.toSquare(fid);
  }
}
