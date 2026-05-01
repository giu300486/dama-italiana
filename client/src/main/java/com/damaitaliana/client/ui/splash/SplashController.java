package com.damaitaliana.client.ui.splash;

import com.damaitaliana.client.app.SceneId;
import com.damaitaliana.client.app.SceneRouter;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.client.persistence.AutosaveService;
import java.util.Objects;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Controller backing {@code splash.fxml}. Schedules a small bootstrap pipeline on a virtual thread
 * (so the JavaFX application thread stays free to draw the indeterminate {@link
 * javafx.scene.control.ProgressIndicator}) and routes to the main menu when finished.
 *
 * <p>The pipeline does only what cannot be done eagerly during Spring context init: it asks {@link
 * AutosaveService#autosaveExists()} whether an autosave file is on disk and signals the next scene
 * via {@link SceneRouter#setAutosavePromptOnNext(boolean)}. Theme stylesheets and the active locale
 * are already applied by the time this controller runs (theme via {@link
 * com.damaitaliana.client.app.ThemeService#applyTheme(javafx.scene.Scene)} on each {@code
 * SceneRouter.show}, locale via {@link com.damaitaliana.client.i18n.LocaleService}'s constructor
 * that reads from {@link com.damaitaliana.client.persistence.PreferencesService}).
 *
 * <p>{@link #MIN_SPLASH_MILLIS} guarantees the splash stays visible for at least 1.5s even on fast
 * machines, matching SPEC §9.1's "1-2 secondi" expectation.
 */
@Component
@Scope("prototype")
public class SplashController {

  private static final Logger log = LoggerFactory.getLogger(SplashController.class);
  static final long MIN_SPLASH_MILLIS = 1500L;

  private final SceneRouter sceneRouter;
  private final AutosaveService autosaveService;
  private final I18n i18n;

  @FXML private Label loadingLabel;
  @FXML private Label subtitleLabel;

  public SplashController(SceneRouter sceneRouter, AutosaveService autosaveService, I18n i18n) {
    this.sceneRouter = Objects.requireNonNull(sceneRouter, "sceneRouter");
    this.autosaveService = Objects.requireNonNull(autosaveService, "autosaveService");
    this.i18n = Objects.requireNonNull(i18n, "i18n");
  }

  @FXML
  void initialize() {
    loadingLabel.setText(i18n.t("splash.loading"));
    subtitleLabel.setText(i18n.t("splash.subtitle"));
    Task<BootstrapResult> task =
        new Task<>() {
          @Override
          protected BootstrapResult call() throws InterruptedException {
            return bootstrap();
          }
        };
    task.setOnSucceeded(ev -> finish(task.getValue()));
    task.setOnFailed(
        ev -> {
          log.warn("Splash bootstrap failed; skipping prompt", task.getException());
          finish(new BootstrapResult(false));
        });
    Thread.ofVirtual().name("splash-bootstrap").start(task);
  }

  /** Synchronous bootstrap work. Visible for testing. */
  BootstrapResult bootstrap() throws InterruptedException {
    long start = System.currentTimeMillis();
    boolean hasAutosave = autosaveService.autosaveExists();
    long elapsed = System.currentTimeMillis() - start;
    long remaining = Math.max(0, MIN_SPLASH_MILLIS - elapsed);
    if (remaining > 0) {
      Thread.sleep(remaining);
    }
    return new BootstrapResult(hasAutosave);
  }

  /**
   * Called once {@link #bootstrap()} resolves. The FX-side production caller is {@link
   * Task#setOnSucceeded}, which the JavaFX runtime guarantees runs on the application thread.
   * Visible for testing.
   */
  void finish(BootstrapResult result) {
    if (result.hasAutosave()) {
      sceneRouter.setAutosavePromptOnNext(true);
    }
    sceneRouter.show(SceneId.MAIN_MENU);
  }

  /** Carrier for the small slice of state the bootstrap step needs to publish to the FX thread. */
  record BootstrapResult(boolean hasAutosave) {}
}
