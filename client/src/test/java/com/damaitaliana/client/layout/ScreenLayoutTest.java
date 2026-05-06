package com.damaitaliana.client.layout;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * F4.5 Task 4.5.6 — verifies the responsive width / height budgets the FXMLs of the menu, the 5
 * form screens and the board view declare for their main content container. The audit at Task 4.5.2
 * documented the stretch defect on ultrawide / 4K viewports; these caps are what stops the 5-card
 * menu, the setup card and the rules split from filling the whole viewport.
 *
 * <p>Tests load each FXML with {@code loader.setControllerFactory(Mockito::mock)} so no Spring
 * context is required (same pattern as {@code FxmlLoadingSmokeTest}). The FXML namespace is
 * inspected directly via {@link FXMLLoader#getNamespace()} which returns the {@code fx:id} → Node
 * map, sidestepping the need for a CSS-applied scene before lookup works.
 */
class ScreenLayoutTest {

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
  void mainMenuGridCapsAtReadableWidth() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable");

    GridPane menuGrid = (GridPane) loadAndLookup("/fxml/main-menu.fxml", "menuGrid");
    assertThat(menuGrid.getMaxWidth()).isEqualTo(960.0);
    assertThat(menuGrid.getMaxHeight()).isEqualTo(Region.USE_PREF_SIZE);
  }

  @Test
  void singlePlayerSetupCardCapsAtReadableWidthAndPrefHeight() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable");

    VBox card = (VBox) loadAndLookup("/fxml/sp-setup.fxml", "setupCard");
    assertThat(card.getMaxWidth()).isEqualTo(560.0);
    assertThat(card.getMaxHeight()).isEqualTo(Region.USE_PREF_SIZE);
  }

  @Test
  void settingsCardCapsAtReadableWidthAndPrefHeight() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable");

    VBox card = (VBox) loadAndLookup("/fxml/settings.fxml", "settingsCard");
    assertThat(card.getMaxWidth()).isEqualTo(560.0);
    assertThat(card.getMaxHeight()).isEqualTo(Region.USE_PREF_SIZE);
  }

  @Test
  void loadScreenTableCapsAtReadableWidth() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable");

    TableView<?> table = (TableView<?>) loadAndLookup("/fxml/load-screen.fxml", "slotsTable");
    assertThat(table.getMaxWidth()).isEqualTo(1100.0);
  }

  @Test
  void rulesSplitPaneCapsAtReadableWidth() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable");

    Object rulesCenter = loadAndLookup("/fxml/rules.fxml", "rulesCenter");
    SplitPane splitPane =
        (SplitPane)
            ((javafx.scene.layout.StackPane) rulesCenter)
                .getChildren().stream()
                    .filter(SplitPane.class::isInstance)
                    .findFirst()
                    .orElseThrow();
    assertThat(splitPane.getMaxWidth()).isEqualTo(1200.0);
  }

  // save-dialog deliberately omitted: its FXML has no fx:controller (the modal stage installs
  // SaveDialogController programmatically via loader.setController(...)). Task 4.5.5 verified
  // that the modal uses setResizable(false), so no responsive-width cap is needed here.

  private static Object loadAndLookup(String fxmlPath, String fxId) throws Exception {
    return runOnFxThread(
        () -> {
          FXMLLoader loader = new FXMLLoader(ScreenLayoutTest.class.getResource(fxmlPath));
          loader.setControllerFactory(Mockito::mock);
          loader.load();
          Object node = loader.getNamespace().get(fxId);
          if (node == null) {
            throw new IllegalStateException(
                "fx:id '" + fxId + "' not found in namespace of " + fxmlPath);
          }
          return node;
        });
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
