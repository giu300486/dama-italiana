package com.damaitaliana.client.layout;

import com.damaitaliana.client.ClientApplication;
import com.damaitaliana.client.app.ThemeService;
import com.damaitaliana.client.controller.GameSession;
import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.client.ui.save.SaveDialogController;
import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

/**
 * F4.5 Task 4.5.2 — automated baseline screenshot capture (pre-fix). For each F3.5 screen × each
 * canonical desktop resolution, loads the FXML via Spring controller factory, applies the active
 * theme, sizes the {@link Scene}, forces a layout pulse, and writes a PNG snapshot to {@code
 * tests/visual-review/responsiveness-baseline-pre-fix/}. The 56 baseline images document the defect
 * surface area before the F4.5 layout refactor (Task 4.5.4 onwards) so post-fix screenshots (Task
 * 4.5.9) can be diffed for regression reasoning.
 *
 * <p>Tagged {@code slow} so the regular {@code mvn -pl client verify -DexcludedGroups=slow,
 * performance} fast loop skips it; the closure regression {@code mvn clean verify} root exercises
 * it (and the post-fix variant in Task 4.5.9 will also be slow-tagged).
 *
 * <p><b>DPI scaling note</b>: JavaFX 21 reads the render scale from JVM startup args ({@code
 * -Dprism.allowhidpi=true}, {@code -Dglass.win.uiScale=N}); a single JVM run cannot exercise
 * multiple scaling factors. The 7 canonical resolutions here all assume 100% logical DPI; the
 * 125/150/200% variants of the SPEC §13.7 / NFR-U-05 matrix are verified post-fix by manual
 * launches with the appropriate JVM args (documented in TEST-PLAN-fase-4.5 §5.2) — the automated
 * layout calculation is DPI-independent so a single-JVM run still validates the responsive math
 * correctness.
 *
 * <p><b>Headless behaviour</b>: relies on the same {@code Platform.startup} pattern as the existing
 * {@code *FxmlSmokeTest} classes (e.g. {@link
 * com.damaitaliana.client.ui.settings.SettingsFxmlSmokeTest}). When the JavaFX toolkit cannot boot
 * (CI without display, locked-down sandbox), the test self-skips via {@link
 * Assumptions#assumeTrue(boolean, String)}.
 */
@SpringBootTest(
    classes = ClientApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {"dama.client.saves-dir=/tmp/test-saves-baseline-screenshot"})
@Tag("slow")
class BaselineScreenshotCapture {

  private static final Path OUT_DIR =
      Paths.get("..", "tests", "visual-review", "responsiveness-baseline-pre-fix");

  /** Logical-pixel viewport sizes (no DPI scaling — see class Javadoc). */
  private record Resolution(int width, int height, String label) {}

  private static final List<Resolution> RESOLUTIONS =
      List.of(
          new Resolution(1024, 720, "1024x720-floor"),
          new Resolution(1280, 720, "1280x720-small"),
          new Resolution(1366, 768, "1366x768-laptop"),
          new Resolution(1920, 1080, "1920x1080-fhd"),
          new Resolution(2560, 1440, "2560x1440-qhd"),
          new Resolution(3440, 1440, "3440x1440-ultrawide"),
          new Resolution(3840, 2160, "3840x2160-4k"));

  /**
   * @param needsGameSession {@code true} when the controller's {@code initialize()} requires a
   *     started single-player game in {@link GameSession} — currently only board-view (which
   *     redirects to MAIN_MENU when the session is empty) and save-dialog (which is owned by
   *     board-view).
   * @param controllerInFxml {@code true} when the FXML declares {@code fx:controller=...} — the
   *     loader uses {@link FXMLLoader#setControllerFactory}; otherwise the test passes a Spring
   *     bean via {@link FXMLLoader#setController}.
   */
  private record Screen(
      String name, String fxmlPath, boolean needsGameSession, boolean controllerInFxml) {}

  private static final List<Screen> SCREENS =
      List.of(
          new Screen("splash", "/fxml/splash.fxml", false, true),
          new Screen("main-menu", "/fxml/main-menu.fxml", false, true),
          new Screen("sp-setup", "/fxml/sp-setup.fxml", false, true),
          new Screen("board-view", "/fxml/board-view.fxml", true, true),
          new Screen("load-screen", "/fxml/load-screen.fxml", false, true),
          new Screen("settings", "/fxml/settings.fxml", false, true),
          new Screen("rules", "/fxml/rules.fxml", false, true),
          new Screen("save-dialog", "/fxml/save-dialog.fxml", true, false));

  private static boolean fxToolkitReady;

  @Autowired ApplicationContext springContext;
  @Autowired ThemeService themeService;
  @Autowired GameSession gameSession;

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

  static Stream<Arguments> scenarios() {
    return SCREENS.stream()
        .flatMap(screen -> RESOLUTIONS.stream().map(res -> Arguments.of(screen, res)));
  }

  @ParameterizedTest(name = "[{1}] {0}")
  @MethodSource("scenarios")
  void captureBaseline(Screen screen, Resolution res) throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    Files.createDirectories(OUT_DIR);

    // Pre-populate session so board-view's initialize() does not redirect to MAIN_MENU.
    if (screen.needsGameSession()) {
      ensureFakeGameSession();
    }

    runOnFxThread(
        () -> {
          FXMLLoader loader = new FXMLLoader(getClass().getResource(screen.fxmlPath()));
          if (screen.controllerInFxml()) {
            loader.setControllerFactory(springContext::getBean);
          } else if ("save-dialog".equals(screen.name())) {
            // SaveDialogController is a prototype bean instantiated programmatically.
            loader.setController(springContext.getBean(SaveDialogController.class));
          }
          Parent root = loader.load();
          Scene scene = new Scene(root, res.width(), res.height());
          themeService.applyTheme(scene);

          // Force a CSS + layout pulse so the snapshot reflects a settled scene.
          root.applyCss();
          root.layout();

          WritableImage fxImage = scene.snapshot(null);
          BufferedImage bi = toBufferedImage(fxImage);

          Path file = OUT_DIR.resolve(screen.name() + "_" + res.label() + ".png");
          ImageIO.write(bi, "png", file.toFile());
          return null;
        });
  }

  /**
   * Builds an in-memory single-player game from the initial position so {@code
   * BoardViewController.initialize()} finds a current game and renders the board. Reused across all
   * 7 board-view captures (idempotent — overwrites if already set).
   */
  private void ensureFakeGameSession() {
    SinglePlayerGame fake =
        new SinglePlayerGame(
            AiLevel.PRINCIPIANTE,
            Color.WHITE,
            "F4.5 baseline",
            GameState.initial(),
            new SplittableRandom(0L));
    gameSession.setCurrentGame(fake);
  }

  /**
   * Bulk-converts a JavaFX {@link WritableImage} into an AWT {@link BufferedImage} for {@link
   * ImageIO} writing, without depending on {@code javafx-swing} (avoids adding a new dependency
   * outside SPEC §6). 4K bulk transfer is ~100 ms; pixel-by-pixel would be too slow.
   */
  private static BufferedImage toBufferedImage(WritableImage fxImage) {
    int w = (int) fxImage.getWidth();
    int h = (int) fxImage.getHeight();
    int[] argb = new int[w * h];
    fxImage.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), argb, 0, w);
    BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    bi.setRGB(0, 0, w, h, argb, 0, w);
    return bi;
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
    if (!latch.await(20, TimeUnit.SECONDS)) {
      throw new IllegalStateException("FX task did not complete within 20s");
    }
    if (failure.get() != null) {
      throw new RuntimeException(failure.get());
    }
    return holder.get();
  }
}
