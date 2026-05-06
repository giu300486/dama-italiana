package com.damaitaliana.client.ui.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * F4.5 Task 4.5.4 — verifies {@link BoardRenderer} centers the 8×8 grid inside its layout area
 * across the canonical viewport sizes the {@code BoardView} can be allocated. Each parametric run
 * pushes a {@code (width, height)} into the renderer, forces a layout pulse and asserts:
 *
 * <ul>
 *   <li>{@code cellSize == min(w, h) / 8} (BoardLayoutMath invariant);
 *   <li>the centered 8×8 board fits entirely inside {@code (0, 0, w, h)} — no clipping anywhere;
 *   <li>the offset on each axis is {@code (size - 8*cellSize) / 2} so the board sits in the middle
 *       of the wood frame regardless of aspect ratio;
 *   <li>cell (0, 0) (file=0, JavaFX y=top of rank 0 ⇒ bottom row) lives at the expected offset;
 *   <li>the particle layer overlays exactly the centered board area;
 *   <li>cells are square ({@code width == height}).
 * </ul>
 *
 * <p>Headless self-skip via {@link Assumptions#assumeTrue(boolean, String)} matches the existing
 * {@code UiScalingServiceTest} pattern (ADR-018).
 */
class BoardRendererLayoutTest {

  private static boolean fxToolkitReady;

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

  /**
   * Renderer-area sizes corresponding to the area the BoardView's StackPane allocates after
   * subtracting the menu bar, title row, side panel and padding from each canonical Stage size of
   * the F4.5 audit (Task 4.5.2). Approximate, but representative of the aspect ratios we want to
   * cover: square-ish, wide, very-wide ultrawide, and a tall edge case.
   */
  static java.util.stream.Stream<Arguments> rendererAreas() {
    return java.util.stream.Stream.of(
        Arguments.of(560.0, 560.0), // square at the smallest reasonable viewport
        Arguments.of(720.0, 560.0), // 1366×768-laptop after chrome (slightly wider than tall)
        Arguments.of(1024.0, 720.0), // 1920×1080-fhd after chrome
        Arguments.of(1600.0, 1200.0), // 2560×1440-qhd after chrome
        Arguments.of(2400.0, 1200.0), // 3440×1440-ultrawide after chrome (extreme aspect)
        Arguments.of(1800.0, 1800.0), // 3840×2160-4k after chrome (square at 4K)
        Arguments.of(560.0, 720.0)); // taller-than-wide (defensive — never happens in practice)
  }

  @ParameterizedTest(name = "renderer {0}×{1}")
  @MethodSource("rendererAreas")
  void cellsAreCenteredAndFitWithinTheRenderer(double width, double height) throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    BoardRenderer renderer = runOnFxThread(BoardRenderer::new);
    runOnFxThread(
        () -> {
          renderer.resize(width, height);
          renderer.layout();
          return null;
        });

    double cellSize = Math.min(width, height) / 8.0;
    double total = cellSize * 8.0;
    double xOffset = (width - total) / 2.0;
    double yOffset = (height - total) / 2.0;

    Node bottomLeft = cellAt(renderer, 0, 0);
    Node topRight = cellAt(renderer, 7, 7);

    // file=0, rank=0 (bottom-left from White's perspective): JavaFX y = (8-1-0)*cellSize.
    assertThat(bottomLeft.getLayoutX()).isCloseTo(xOffset + 0.0, within(1e-6));
    assertThat(bottomLeft.getLayoutY()).isCloseTo(yOffset + 7.0 * cellSize, within(1e-6));
    assertThat(((javafx.scene.layout.Region) bottomLeft).getWidth())
        .isCloseTo(cellSize, within(1e-6));
    assertThat(((javafx.scene.layout.Region) bottomLeft).getHeight())
        .isCloseTo(cellSize, within(1e-6));

    // file=7, rank=7 (top-right from White's perspective): JavaFX y = (8-1-7)*cellSize = 0.
    assertThat(topRight.getLayoutX()).isCloseTo(xOffset + 7.0 * cellSize, within(1e-6));
    assertThat(topRight.getLayoutY()).isCloseTo(yOffset + 0.0, within(1e-6));

    // No cell extends beyond the renderer rectangle.
    for (int file = 0; file < 8; file++) {
      for (int rank = 0; rank < 8; rank++) {
        Node cell = cellAt(renderer, file, rank);
        assertThat(cell.getLayoutX()).isGreaterThanOrEqualTo(0);
        assertThat(cell.getLayoutY()).isGreaterThanOrEqualTo(0);
        assertThat(cell.getLayoutX() + cellSize).isLessThanOrEqualTo(width + 1e-6);
        assertThat(cell.getLayoutY() + cellSize).isLessThanOrEqualTo(height + 1e-6);
      }
    }

    // Particle layer overlays the centered board area exactly.
    Pane particleHost = renderer.particleHost();
    assertThat(particleHost.getLayoutX()).isCloseTo(xOffset, within(1e-6));
    assertThat(particleHost.getLayoutY()).isCloseTo(yOffset, within(1e-6));
    assertThat(particleHost.getWidth()).isCloseTo(total, within(1e-6));
    assertThat(particleHost.getHeight()).isCloseTo(total, within(1e-6));

    assertThat(renderer.currentCellSize()).isCloseTo(cellSize, within(1e-6));
  }

  private static Node cellAt(BoardRenderer renderer, int file, int rank) {
    // BoardRenderer adds cells in (file, rank) loop order in its constructor; index = file*8 +
    // rank.
    return renderer.getChildrenUnmodifiable().get(file * 8 + rank);
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
