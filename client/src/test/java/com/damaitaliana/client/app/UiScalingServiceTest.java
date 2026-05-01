package com.damaitaliana.client.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.damaitaliana.client.persistence.PreferencesService;
import com.damaitaliana.client.persistence.UserPreferences;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UiScalingServiceTest {

  private static boolean fxToolkitReady;

  private PreferencesService preferencesService;
  private UiScalingService service;

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

  @BeforeEach
  void setUp() {
    preferencesService = Mockito.mock(PreferencesService.class);
    service = new UiScalingService(preferencesService);
  }

  @Test
  void nullSceneIsNoOp() {
    service.applyTo(null, 125);
  }

  @Test
  void appliesPercentToSceneRoot() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    Region root = runOnFxThread(StackPane::new);
    Scene scene = runOnFxThread(() -> new Scene(root));

    runOnFxThread(
        () -> {
          service.applyTo(scene, 125);
          return null;
        });

    assertThat(root.getStyle()).contains("-fx-font-size:").contains("17.5px");
  }

  @Test
  void unknownPercentFallsBackTo100Percent() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    Region root = runOnFxThread(StackPane::new);
    Scene scene = runOnFxThread(() -> new Scene(root));

    runOnFxThread(
        () -> {
          service.applyTo(scene, 200);
          return null;
        });

    assertThat(root.getStyle()).contains("14.0px");
  }

  @Test
  void readsCurrentPreferenceWhenNoExplicitPercent() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    when(preferencesService.load())
        .thenReturn(
            new UserPreferences(
                UserPreferences.CURRENT_SCHEMA_VERSION,
                Locale.ITALIAN,
                "light",
                150,
                false,
                UserPreferences.DEFAULT_MUSIC_VOLUME_PERCENT,
                UserPreferences.DEFAULT_SFX_VOLUME_PERCENT,
                false,
                false));

    Region root = runOnFxThread(StackPane::new);
    Scene scene = runOnFxThread(() -> new Scene(root));

    runOnFxThread(
        () -> {
          service.applyTo(scene);
          return null;
        });

    assertThat(root.getStyle()).contains("21.0px");
  }

  @Test
  void allowedScalesAreThe100_125_150Steps() {
    assertThat(UiScalingService.ALLOWED_SCALES).containsExactly(100, 125, 150);
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
