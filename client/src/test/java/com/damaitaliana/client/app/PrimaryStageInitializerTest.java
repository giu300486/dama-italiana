package com.damaitaliana.client.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * F4.5 Task 4.5.3 — verifies the primary Stage F4.5 responsive baseline: min size, initial size
 * computed from the primary screen visualBounds, position centered. Headless self-skip via {@link
 * Assumptions#assumeTrue(boolean, String)} matches the existing {@code UiScalingServiceTest}
 * pattern (ADR-018).
 */
class PrimaryStageInitializerTest {

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

  @Test
  void nullStageThrowsNpe() {
    assertThatNullPointerException()
        .isThrownBy(() -> new PrimaryStageInitializer().initialize(null));
  }

  @Test
  void minSizeMatchesF45Floor() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    Stage stage = runOnFxThread(Stage::new);

    runOnFxThread(
        () -> {
          new PrimaryStageInitializer().initialize(stage);
          return null;
        });

    assertThat(stage.getMinWidth()).isEqualTo(PrimaryStageInitializer.MIN_WIDTH);
    assertThat(stage.getMinHeight()).isEqualTo(PrimaryStageInitializer.MIN_HEIGHT);
  }

  @Test
  void initialSizeIsAtLeastMinAndComputedAt80PercentOfPrimaryScreen() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    Stage stage = runOnFxThread(Stage::new);
    Rectangle2D bounds = runOnFxThread(() -> Screen.getPrimary().getVisualBounds());

    runOnFxThread(
        () -> {
          new PrimaryStageInitializer().initialize(stage);
          return null;
        });

    double ratio = PrimaryStageInitializer.INITIAL_SIZE_RATIO_PERCENT / 100.0;
    double expectedWidth = Math.max(PrimaryStageInitializer.MIN_WIDTH, bounds.getWidth() * ratio);
    double expectedHeight =
        Math.max(PrimaryStageInitializer.MIN_HEIGHT, bounds.getHeight() * ratio);
    assertThat(stage.getWidth()).isEqualTo(expectedWidth);
    assertThat(stage.getHeight()).isEqualTo(expectedHeight);
    assertThat(stage.getWidth()).isGreaterThanOrEqualTo(stage.getMinWidth());
    assertThat(stage.getHeight()).isGreaterThanOrEqualTo(stage.getMinHeight());
  }

  @Test
  void positionIsCenteredOnPrimaryScreenVisualBounds() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    Stage stage = runOnFxThread(Stage::new);
    Rectangle2D bounds = runOnFxThread(() -> Screen.getPrimary().getVisualBounds());

    runOnFxThread(
        () -> {
          new PrimaryStageInitializer().initialize(stage);
          return null;
        });

    double expectedX = bounds.getMinX() + (bounds.getWidth() - stage.getWidth()) / 2;
    double expectedY = bounds.getMinY() + (bounds.getHeight() - stage.getHeight()) / 2;
    assertThat(stage.getX()).isEqualTo(expectedX);
    assertThat(stage.getY()).isEqualTo(expectedY);
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
