package com.damaitaliana.client.ui.board.animation;

import com.damaitaliana.client.ui.board.BoardLayoutMath;
import com.damaitaliana.shared.domain.CaptureSequence;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.SimpleMove;
import com.damaitaliana.shared.domain.Square;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * Composes the {@link MoveAnimator} primitives into the {@link Animation} for a full {@link Move}.
 *
 * <p>Output shape:
 *
 * <ul>
 *   <li>{@link SimpleMove} → a single {@link TranslateTransition} from {@code from} to {@code to}.
 *   <li>{@link CaptureSequence} → a {@link SequentialTransition} of legs; each leg is a {@link
 *       ParallelTransition} of the slide for that leg and the fade-out of the captured piece on
 *       that leg.
 * </ul>
 *
 * <p>The moving piece's {@link Node} is resolved once at the start; each leg applies an incremental
 * {@code byX/byY} translate to the same node, so the cumulative effect produces the full path the
 * human eye expects to follow.
 *
 * <p>Stateless utility — kept off {@link com.damaitaliana.client.ui.board.BoardRenderer} so unit
 * tests can exercise the composition by passing a {@code Function<Square, Node>} resolver.
 */
public final class AnimationOrchestrator {

  private AnimationOrchestrator() {}

  /**
   * Builds the full {@link Animation} for {@code move}.
   *
   * @param move the move to visualise.
   * @param pieceAt resolves a {@link Square} to the {@link Node} currently rendered there. The
   *     orchestrator queries it for the moving piece (at {@code move.from()}) and for each captured
   *     piece. Production wires this to {@code BoardRenderer}'s cell lookup; tests can pass a
   *     deterministic resolver returning placeholder shapes.
   * @param cellSize cell side length in pixels — used to convert file/rank deltas to pixel offsets
   *     via {@link BoardLayoutMath#xFor(int, double)} / {@link BoardLayoutMath#yFor(int, double)}.
   */
  public static Animation animateMove(Move move, Function<Square, Node> pieceAt, double cellSize) {
    return animateMove(move, pieceAt, cellSize, null);
  }

  /**
   * Same as {@link #animateMove(Move, Function, double)} but, when {@code particleHost} is
   * non-null, layers a {@link ParticleEffects#captureSplash} on each capture leg (Task 3.5.8).
   * Splash particles are spawned at the captured square's centre so the user sees a brown/grey dust
   * burst as the captured piece dissolves.
   */
  public static Animation animateMove(
      Move move, Function<Square, Node> pieceAt, double cellSize, Pane particleHost) {
    Objects.requireNonNull(move, "move");
    Objects.requireNonNull(pieceAt, "pieceAt");
    if (cellSize <= 0) {
      throw new IllegalArgumentException("cellSize must be positive: " + cellSize);
    }

    Node movingPiece =
        Objects.requireNonNull(pieceAt.apply(move.from()), () -> "no piece node at " + move.from());

    if (move instanceof SimpleMove sm) {
      return slideLeg(movingPiece, sm.from(), sm.to(), cellSize);
    }
    if (move instanceof CaptureSequence cs) {
      return sequenceForCapture(cs, movingPiece, pieceAt, cellSize, particleHost);
    }
    throw new IllegalArgumentException("Unsupported move kind: " + move.getClass());
  }

  private static TranslateTransition slideLeg(
      Node movingPiece, Square from, Square to, double cellSize) {
    double dx = (to.file() - from.file()) * cellSize;
    // JavaFX y axis is top-down; rank delta has to be negated because rank 0 is at the bottom.
    double dy = -(to.rank() - from.rank()) * cellSize;
    return MoveAnimator.slideMove(movingPiece, dx, dy);
  }

  private static SequentialTransition sequenceForCapture(
      CaptureSequence cs,
      Node movingPiece,
      Function<Square, Node> pieceAt,
      double cellSize,
      Pane particleHost) {
    List<Animation> legs = new ArrayList<>();
    Square cursor = cs.from();
    for (int i = 0; i < cs.path().size(); i++) {
      Square landing = cs.path().get(i);
      Square capturedSquare = cs.captured().get(i);

      TranslateTransition slide = slideLeg(movingPiece, cursor, landing, cellSize);
      Node capturedNode = pieceAt.apply(capturedSquare);
      Animation legAnim;
      if (capturedNode == null) {
        legAnim = slide;
      } else {
        List<Animation> parts = new ArrayList<>();
        parts.add(slide);
        parts.add(MoveAnimator.fadeCapture(capturedNode));
        if (particleHost != null) {
          double cx = BoardLayoutMath.xFor(capturedSquare.file(), cellSize) + cellSize / 2.0;
          double cy = BoardLayoutMath.yFor(capturedSquare.rank(), cellSize) + cellSize / 2.0;
          parts.add(ParticleEffects.captureSplash(particleHost, cx, cy));
        }
        legAnim = new ParallelTransition(parts.toArray(Animation[]::new));
      }
      legs.add(legAnim);
      cursor = landing;
    }
    return new SequentialTransition(legs.toArray(Animation[]::new));
  }
}
