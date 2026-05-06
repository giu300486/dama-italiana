package com.damaitaliana.client.layout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.damaitaliana.client.ui.board.BoardRenderer;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * F4.5 Task 4.5.8 — verifies the proportional frame-thickness math (pure unit, no FX) and
 * end-to-end binding applied through {@link BoardFrameThicknessHelper#bindFrameThickness} on a real
 * {@link BoardRenderer} + {@link StackPane} pair.
 *
 * <p>The math tests run headless. The FX-binding tests boot the JavaFX toolkit and self-skip if
 * unavailable (ADR-018 pattern, identical to {@code BoardRendererLayoutTest}).
 */
class BoardFrameThicknessHelperTest {

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

  // -------- pure math (no FX toolkit) ----------------------------------------------------------

  static java.util.stream.Stream<Arguments> thicknessClampPoints() {
    // PLAN-fase-4.5 §4.5.8 spec: clamp(16, boardSide × 0.035, 48).
    return java.util.stream.Stream.of(
        Arguments.of(400.0, 16.0), // 14.0  → clamp 16 (very small board, defensive)
        Arguments.of(460.0, 16.1), // 16.10 just past floor
        Arguments.of(560.0, 19.6), // 1024×720 floor renderer area
        Arguments.of(880.0, 30.8), // 1920×1080 board side
        Arguments.of(1370.0, 47.95), // just under cap (fluid)
        Arguments.of(1372.0, 48.0), // exact cap (boundary)
        Arguments.of(1800.0, 48.0)); // 4K board side → clamp 48
  }

  @ParameterizedTest(name = "thickness @ boardSide={0} → {1}px")
  @MethodSource("thicknessClampPoints")
  void thicknessFollowsClampFormula(double boardSide, double expectedPx) {
    double actual =
        BoardFrameThicknessHelper.computeFrameThickness(
            boardSide,
            BoardFrameThicknessHelper.THICKNESS_MIN_PX,
            BoardFrameThicknessHelper.THICKNESS_MAX_PX,
            BoardFrameThicknessHelper.THICKNESS_SCALE);
    assertThat(actual).isCloseTo(expectedPx, within(1e-6));
  }

  // -------- scene binding (FX toolkit) ---------------------------------------------------------

  @Test
  void bindFrameThicknessAppliesProportionalPaddingAndReactsToResize() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    BoardRenderer renderer = runOnFxThread(BoardRenderer::new);
    StackPane frame = runOnFxThread(StackPane::new);

    runOnFxThread(
        () -> {
          BoardFrameThicknessHelper.bindFrameThickness(frame, renderer);
          return null;
        });

    assertThat(frame.paddingProperty().isBound()).isTrue();

    // 1024×720 floor renderer area → board side 720 → 720×0.035 = 25.2.
    runOnFxThread(
        () -> {
          renderer.resize(1024.0, 720.0);
          return null;
        });
    assertThat(frame.getPadding().getTop()).isCloseTo(25.2, within(1e-6));
    assertThat(frame.getPadding().getBottom()).isCloseTo(25.2, within(1e-6));
    assertThat(frame.getPadding().getLeft()).isCloseTo(25.2, within(1e-6));
    assertThat(frame.getPadding().getRight()).isCloseTo(25.2, within(1e-6));

    // Resize to 4K renderer area → board side 1800 → clamp to 48.
    runOnFxThread(
        () -> {
          renderer.resize(1800.0, 1800.0);
          return null;
        });
    assertThat(frame.getPadding().getTop()).isCloseTo(48.0, within(1e-6));

    // Tiny renderer (defensive) → board side 400 → clamp to floor 16.
    runOnFxThread(
        () -> {
          renderer.resize(400.0, 400.0);
          return null;
        });
    assertThat(frame.getPadding().getTop()).isCloseTo(16.0, within(1e-6));
  }

  @Test
  void bindFrameThicknessIsIdempotentAndUnbindsBeforeRebinding() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    BoardRenderer renderer = runOnFxThread(BoardRenderer::new);
    StackPane frame = runOnFxThread(StackPane::new);

    runOnFxThread(
        () -> {
          BoardFrameThicknessHelper.bindFrameThickness(frame, renderer);
          // Second call must NOT throw "A bound value cannot be set" or duplicate.
          BoardFrameThicknessHelper.bindFrameThickness(frame, renderer);
          renderer.resize(880.0, 880.0);
          return null;
        });

    assertThat(frame.paddingProperty().isBound()).isTrue();
    assertThat(frame.getPadding().getTop()).isCloseTo(30.8, within(1e-6));
  }

  @Test
  void bindFrameThicknessUsesMinOfWidthAndHeightSoUltrawideRendererStillSizesByBoardSide()
      throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    BoardRenderer renderer = runOnFxThread(BoardRenderer::new);
    StackPane frame = runOnFxThread(StackPane::new);

    runOnFxThread(
        () -> {
          BoardFrameThicknessHelper.bindFrameThickness(frame, renderer);
          // Ultrawide-after-chrome renderer area: 2400×1200 → board side = min = 1200 → 42.0.
          renderer.resize(2400.0, 1200.0);
          return null;
        });

    assertThat(frame.getPadding().getTop()).isCloseTo(42.0, within(1e-6));
  }

  private static <T> T runOnFxThread(Callable<T> task) throws Exception {
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
