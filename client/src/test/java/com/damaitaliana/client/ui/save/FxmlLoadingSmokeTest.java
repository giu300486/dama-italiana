package com.damaitaliana.client.ui.save;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.client.app.UserPromptService;
import com.damaitaliana.client.controller.ColorChoice;
import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.client.persistence.SaveService;
import com.damaitaliana.shared.ai.AiLevel;
import java.util.SplittableRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Loads each Task 3.15 FXML on the JavaFX thread to catch parse / wiring errors that pure unit
 * tests bypass. This is the runtime smoke that {@code mvn javafx:run} would surface as soon as the
 * user opened the save dialog or navigated to the load screen.
 */
class FxmlLoadingSmokeTest {

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
  void saveDialogFxmlLoads() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable");

    SaveService saveService = Mockito.mock(SaveService.class);
    UserPromptService prompt = Mockito.mock(UserPromptService.class);
    I18n i18n = Mockito.mock(I18n.class);
    Mockito.when(i18n.t(Mockito.anyString())).thenAnswer(inv -> inv.getArgument(0));
    Mockito.when(i18n.t(Mockito.anyString(), Mockito.any(Object[].class)))
        .thenAnswer(inv -> inv.getArgument(0));
    SaveDialogController controller = new SaveDialogController(saveService, prompt, i18n);
    SinglePlayerGame snapshot =
        SinglePlayerGame.tryCreate(
                AiLevel.ESPERTO, ColorChoice.WHITE, "Test", new SplittableRandom(42L))
            .orElseThrow();
    controller.setSnapshotForTest(snapshot);

    Parent root =
        runOnFxThread(
            () -> {
              FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/save-dialog.fxml"));
              loader.setController(controller);
              return loader.load();
            });

    assertThat(root).isNotNull();
  }

  @Test
  void boardViewFxmlLoads() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable");

    Parent root =
        runOnFxThread(
            () -> {
              FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/board-view.fxml"));
              loader.setControllerFactory(Mockito::mock);
              return loader.load();
            });

    assertThat(root).isNotNull();
  }

  @Test
  void loadScreenFxmlLoads() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable");

    Parent root =
        runOnFxThread(
            () -> {
              FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/load-screen.fxml"));
              loader.setControllerFactory(Mockito::mock);
              return loader.load();
            });

    assertThat(root).isNotNull();
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
