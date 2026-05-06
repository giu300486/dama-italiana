package com.damaitaliana.client.ui.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

/**
 * F4.5 Task 4.5.5 — verifies the board-view side panel respects the responsive width budget
 * (pref=320, min=240, max=400) declared in {@code board-view.fxml}.
 *
 * <p>The first test checks the static FXML-declared constraints. The parametric test resizes a
 * synthetic Scene at the 5 canonical Stage widths of the F4.5 audit and asserts the actual width
 * the side panel settles to falls within {@code [240, 400]} regardless of how generous the viewport
 * is — i.e. the panel does not grow indefinitely on ultrawide and 4K nor shrink below 240 at the
 * floor.
 *
 * <p>Headless self-skip via {@link Assumptions#assumeTrue(boolean, String)} matches the existing
 * {@code FxmlLoadingSmokeTest} pattern (ADR-018).
 */
class SidePanelLayoutTest {

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
  void sidePanelDeclaresResponsiveWidthBudget() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable");

    VBox sidePanel = runOnFxThread(SidePanelLayoutTest::loadSidePanel);

    assertThat(sidePanel.getPrefWidth()).isEqualTo(320.0);
    assertThat(sidePanel.getMinWidth()).isEqualTo(240.0);
    assertThat(sidePanel.getMaxWidth()).isEqualTo(400.0);
  }

  static java.util.stream.Stream<Arguments> stageWidths() {
    return java.util.stream.Stream.of(
        Arguments.of(1024.0), // floor — Stage minimum (PrimaryStageInitializer)
        Arguments.of(1366.0), // likely customer laptop
        Arguments.of(1920.0), // FHD desktop
        Arguments.of(2560.0), // QHD
        Arguments.of(3440.0), // ultrawide
        Arguments.of(3840.0)); // 4K
  }

  @ParameterizedTest(name = "stageWidth={0}")
  @MethodSource("stageWidths")
  void sidePanelStaysWithinBudgetAcrossStageWidths(double stageWidth) throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable");

    Parent root = runOnFxThread(SidePanelLayoutTest::loadBoardView);
    Scene scene = runOnFxThread(() -> new Scene(root, stageWidth, 720.0));

    runOnFxThread(
        () -> {
          root.applyCss();
          root.layout();
          return null;
        });

    VBox sidePanel = (VBox) scene.lookup("#sidePanel");
    assertThat(sidePanel).isNotNull();
    assertThat(sidePanel.getWidth())
        .as("side panel width at stageWidth=%s must stay in [240, 400]", stageWidth)
        .isGreaterThanOrEqualTo(240.0 - 1e-6)
        .isLessThanOrEqualTo(400.0 + 1e-6);
    // Default BorderPane.right behaviour: child sizes to its prefWidth (capped at maxWidth)
    // independent of viewport — so we expect to settle at 320 once layout pulses.
    assertThat(sidePanel.getWidth())
        .as("side panel should settle at prefWidth=320 across all canonical viewports")
        .isCloseTo(320.0, within(1.0));
  }

  private static VBox loadSidePanel() throws Exception {
    Parent root = loadBoardView();
    Object node = ((javafx.scene.Parent) root).lookup("#sidePanel");
    if (node == null) {
      // Lookup before CSS pulse can fail; fall back to FXML namespace via fresh load.
      FXMLLoader loader =
          new FXMLLoader(SidePanelLayoutTest.class.getResource("/fxml/board-view.fxml"));
      loader.setControllerFactory(Mockito::mock);
      loader.load();
      return (VBox) loader.getNamespace().get("sidePanel");
    }
    return (VBox) node;
  }

  private static Parent loadBoardView() throws Exception {
    FXMLLoader loader =
        new FXMLLoader(SidePanelLayoutTest.class.getResource("/fxml/board-view.fxml"));
    loader.setControllerFactory(Mockito::mock);
    return loader.load();
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
