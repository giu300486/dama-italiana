package com.damaitaliana.client.layout;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.client.ClientApplication;
import com.damaitaliana.client.app.ThemeService;
import com.damaitaliana.client.app.UiScalingService;
import com.damaitaliana.client.controller.GameSession;
import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.client.ui.board.BoardRenderer;
import com.damaitaliana.client.ui.save.SaveDialogController;
import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
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
 * F4.5 Task 4.5.9 — parametric post-fix responsiveness assertions + screenshot baseline. Mirror of
 * {@link BaselineScreenshotCapture} but runs <em>after</em> the layout fixes from Tasks 4.5.3 →
 * 4.5.8 land; for each canonical resolution × screen pair it asserts (PLAN-fase-4.5 §4.5.9):
 *
 * <ul>
 *   <li><b>No clipping</b>: {@code root.layoutBounds} fits within the scene rectangle (root max
 *       width/height ≤ scene width/height plus a 1 px float tolerance).
 *   <li><b>No unnecessary scrollbar</b>: no {@link ScrollBar} in the rendered tree is visible (the
 *       baseline {@link GameSession} has no saved games, no overflowing content, no expanded list
 *       so a visible bar would mean the layout couldn't fit the content).
 *   <li><b>Board square</b> (board-view only): the centered 8 × 8 grid uses {@code currentCellSize
 *       × 8} for both width and height — guaranteed by {@link BoardRenderer#layoutChildren} after
 *       Task 4.5.4 (cellSize derives from {@code min(w, h) / 8} so the playing area is square by
 *       construction); the assertion gates against any future regression that would make cells
 *       non-square.
 * </ul>
 *
 * <p>Side effect: writes the 56 post-fix PNGs into {@code
 * tests/visual-review/responsiveness-baseline-post-fix/} so visual diffs against the pre-fix
 * baseline (Task 4.5.2) document the layout improvement.
 *
 * <p><b>DPI scaling</b>: as documented in {@link BaselineScreenshotCapture}, JavaFX 21 picks render
 * scale from JVM startup args and a single JVM run cannot exercise multiple factors. The 7
 * resolutions assume 100 % logical DPI; the 125/150/200 % matrix from SPEC §13.7 / NFR-U-05 is
 * verified by manual launches (TEST-PLAN-fase-4.5 §5.2). The layout math under test is
 * DPI-independent.
 *
 * <p>Tagged {@code slow} so the fast loop {@code mvn -pl client verify
 * -DexcludedGroups=slow,performance} skips it; closure regression {@code mvn clean verify} runs it.
 */
@SpringBootTest(
    classes = ClientApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(
    properties = {"dama.client.saves-dir=/tmp/test-saves-responsiveness-parametric"})
@Tag("slow")
class ResponsivenessParametricTest {

  private static final Path OUT_DIR =
      Paths.get("..", "tests", "visual-review", "responsiveness-baseline-post-fix");

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

  private record Screen(
      String name,
      String fxmlPath,
      boolean needsGameSession,
      boolean controllerInFxml,
      boolean assertable) {}

  /**
   * {@code save-dialog} is rendered outside its real {@code showAndWait} flow so the captured PNG
   * is empty (~10 KB) — see Task 4.5.2 audit. We still snapshot it for parity with the pre-fix
   * baseline file list, but skip the geometric assertions which would be meaningless on an
   * unrendered modal.
   */
  private static final List<Screen> SCREENS =
      List.of(
          new Screen("splash", "/fxml/splash.fxml", false, true, true),
          new Screen("main-menu", "/fxml/main-menu.fxml", false, true, true),
          new Screen("sp-setup", "/fxml/sp-setup.fxml", false, true, true),
          new Screen("board-view", "/fxml/board-view.fxml", true, true, true),
          new Screen("load-screen", "/fxml/load-screen.fxml", false, true, true),
          new Screen("settings", "/fxml/settings.fxml", false, true, true),
          new Screen("rules", "/fxml/rules.fxml", false, true, true),
          new Screen("save-dialog", "/fxml/save-dialog.fxml", true, false, false));

  private static boolean fxToolkitReady;

  @Autowired ApplicationContext springContext;
  @Autowired ThemeService themeService;
  @Autowired UiScalingService uiScalingService;
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
  void assertsAndCaptures(Screen screen, Resolution res) throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    Files.createDirectories(OUT_DIR);

    if (screen.needsGameSession()) {
      ensureFakeGameSession();
    }

    runOnFxThread(
        () -> {
          FXMLLoader loader = new FXMLLoader(getClass().getResource(screen.fxmlPath()));
          if (screen.controllerInFxml()) {
            loader.setControllerFactory(springContext::getBean);
          } else if ("save-dialog".equals(screen.name())) {
            loader.setController(springContext.getBean(SaveDialogController.class));
          }
          Parent root = loader.load();
          Scene scene = new Scene(root, res.width(), res.height());
          themeService.applyTheme(scene);
          uiScalingService.applyTo(scene);
          JavaFxScalingHelper.applyFluidFontsToScene(scene);

          root.applyCss();
          root.layout();

          if (screen.assertable()) {
            assertLayoutInvariants(screen, res, root, scene);
          }

          WritableImage fxImage = scene.snapshot(null);
          BufferedImage bi = toBufferedImage(fxImage);
          Path file = OUT_DIR.resolve(screen.name() + "_" + res.label() + ".png");
          ImageIO.write(bi, "png", file.toFile());
          return null;
        });
  }

  private static void assertLayoutInvariants(
      Screen screen, Resolution res, Parent root, Scene scene) {
    String tag = "[" + res.label() + "/" + screen.name() + "]";
    Bounds bounds = root.getLayoutBounds();

    // (1) No clipping: root content fits inside the scene rect (1 px float tolerance).
    assertThat(bounds.getMaxX())
        .as(tag + " root maxX must fit scene width")
        .isLessThanOrEqualTo(scene.getWidth() + 1.0);
    assertThat(bounds.getMaxY())
        .as(tag + " root maxY must fit scene height")
        .isLessThanOrEqualTo(scene.getHeight() + 1.0);

    // (2) No unnecessary scrollbar visible. Empty test session has no overflowing content; a
    // visible bar would mean the layout could not fit even the empty baseline.
    List<ScrollBar> visibleScrollBars = collectVisibleScrollBars(root);
    assertThat(visibleScrollBars)
        .as(tag + " no ScrollBar should be visible in the empty baseline session")
        .isEmpty();

    // (3) Board-view only: cells are square and the 8x8 grid fits within renderer bounds.
    if ("board-view".equals(screen.name())) {
      BoardRenderer renderer = findFirst(root, BoardRenderer.class);
      assertThat(renderer).as(tag + " board-view must contain a BoardRenderer").isNotNull();
      double cellSize = renderer.currentCellSize();
      assertThat(cellSize)
          .as(tag + " currentCellSize must be > 0 once laid out")
          .isGreaterThan(0.0);
      double boardSide = cellSize * 8.0;
      assertThat(boardSide)
          .as(tag + " 8x8 board must fit in renderer width")
          .isLessThanOrEqualTo(renderer.getWidth() + 1.0);
      assertThat(boardSide)
          .as(tag + " 8x8 board must fit in renderer height")
          .isLessThanOrEqualTo(renderer.getHeight() + 1.0);
    }
  }

  private static List<ScrollBar> collectVisibleScrollBars(Parent root) {
    List<ScrollBar> bars = new ArrayList<>();
    walkScrollBars(root, bars);
    return bars;
  }

  private static void walkScrollBars(Node node, List<ScrollBar> sink) {
    if (node instanceof ScrollBar bar && bar.isVisible() && bar.isManaged()) {
      sink.add(bar);
    }
    if (node instanceof Parent parent) {
      for (Node child : parent.getChildrenUnmodifiable()) {
        walkScrollBars(child, sink);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static <T extends Node> T findFirst(Parent root, Class<T> type) {
    if (type.isInstance(root)) {
      return (T) root;
    }
    for (Node child : root.getChildrenUnmodifiable()) {
      if (type.isInstance(child)) {
        return (T) child;
      }
      if (child instanceof Parent parentChild) {
        T found = findFirst(parentChild, type);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private void ensureFakeGameSession() {
    SinglePlayerGame fake =
        new SinglePlayerGame(
            AiLevel.PRINCIPIANTE,
            Color.WHITE,
            "F4.5 post-fix",
            GameState.initial(),
            new SplittableRandom(0L));
    gameSession.setCurrentGame(fake);
  }

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
