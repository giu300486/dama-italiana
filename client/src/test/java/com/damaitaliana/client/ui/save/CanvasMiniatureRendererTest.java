package com.damaitaliana.client.ui.save;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.client.persistence.SerializedGameState;
import com.damaitaliana.shared.domain.GameState;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.image.Image;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CanvasMiniatureRendererTest {

  private static boolean fxToolkitReady;

  @BeforeAll
  static void initToolkit() {
    try {
      Platform.startup(() -> {});
      fxToolkitReady = true;
    } catch (IllegalStateException alreadyStarted) {
      // Another test in the same JVM has already booted the toolkit — fine.
      fxToolkitReady = true;
    } catch (UnsupportedOperationException headless) {
      // No display available (some CI environments). Skip the rendering tests.
      fxToolkitReady = false;
    }
  }

  @Test
  void rejectsNonPositiveSize() {
    SerializedGameState state = SerializedGameState.fromState(GameState.initial());
    CanvasMiniatureRenderer renderer = new CanvasMiniatureRenderer();
    assertThatThrownBy(() -> renderer.render(state, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> renderer.render(state, -10))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void miniatureRendersWithoutErrors() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");
    SerializedGameState state = SerializedGameState.fromState(GameState.initial());
    CanvasMiniatureRenderer renderer = new CanvasMiniatureRenderer();

    Image image = runOnFxThread(() -> renderer.render(state, 64));

    assertThat(image).isNotNull();
    assertThat(image.getWidth()).isEqualTo(64);
    assertThat(image.getHeight()).isEqualTo(64);
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
    if (!latch.await(5, TimeUnit.SECONDS)) {
      throw new IllegalStateException("FX task did not complete within 5s");
    }
    if (failure.get() != null) {
      throw new RuntimeException(failure.get());
    }
    return holder.get();
  }
}
