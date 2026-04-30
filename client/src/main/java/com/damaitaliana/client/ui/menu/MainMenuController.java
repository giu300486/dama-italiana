package com.damaitaliana.client.ui.menu;

import com.damaitaliana.client.app.SceneId;
import com.damaitaliana.client.app.SceneRouter;
import com.damaitaliana.client.app.UserPromptService;
import com.damaitaliana.client.controller.GameSession;
import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.client.persistence.AutosaveService;
import com.damaitaliana.client.persistence.SaveService;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Optional;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Controller backing {@code main-menu.fxml}. Binds card labels to the active locale, leaves the LAN
 * and Online cards disabled with a tooltip that announces the phase in which they will go live, and
 * — when the splash bootstrap detected an autosave on disk — asks the user whether to resume or
 * discard it.
 *
 * <p>Resume flow (Task 3.16): on accept, the {@link AutosaveService} re-materialises the {@link
 * SinglePlayerGame}, publishes it on {@link GameSession}, and routes to the board view. Read
 * failures (unknown {@code schemaVersion} per ADR-031, IO errors) surface as a localized toast and
 * the autosave is cleared so the user sees a clean main menu on the next start.
 */
@Component
@Scope("prototype")
public class MainMenuController {

  private static final Logger log = LoggerFactory.getLogger(MainMenuController.class);

  /** Outcome of the autosave prompt; surfaced for unit tests. */
  enum PromptResult {
    NO_AUTOSAVE,
    RESUMED,
    DISCARDED,
    SCHEMA_MISMATCH,
    IO_ERROR
  }

  private final SceneRouter sceneRouter;
  private final I18n i18n;
  private final AutosaveService autosaveService;
  private final GameSession gameSession;
  private final UserPromptService prompt;

  @FXML private Label singlePlayerTitle;
  @FXML private Label singlePlayerSubtitle;
  @FXML private Button singlePlayerButton;

  @FXML private Label lanTitle;
  @FXML private Label lanSubtitle;
  @FXML private Button lanButton;
  @FXML private Tooltip lanTooltip;

  @FXML private Label onlineTitle;
  @FXML private Label onlineSubtitle;
  @FXML private Button onlineButton;
  @FXML private Tooltip onlineTooltip;

  @FXML private Label rulesTitle;
  @FXML private Label rulesSubtitle;
  @FXML private Button rulesButton;

  @FXML private Label settingsTitle;
  @FXML private Label settingsSubtitle;
  @FXML private Button settingsButton;

  public MainMenuController(
      SceneRouter sceneRouter,
      I18n i18n,
      AutosaveService autosaveService,
      GameSession gameSession,
      UserPromptService prompt) {
    this.sceneRouter = Objects.requireNonNull(sceneRouter, "sceneRouter");
    this.i18n = Objects.requireNonNull(i18n, "i18n");
    this.autosaveService = Objects.requireNonNull(autosaveService, "autosaveService");
    this.gameSession = Objects.requireNonNull(gameSession, "gameSession");
    this.prompt = Objects.requireNonNull(prompt, "prompt");
  }

  @FXML
  void initialize() {
    bindLabels();
    if (sceneRouter.consumeAutosavePromptOnNext()) {
      // Defer past the current SceneRouter.show() call. Running the prompt synchronously
      // here would block FXML loading, and any sceneRouter.show(BOARD) we trigger from the
      // resume branch would be overwritten as soon as the outer show(MAIN_MENU) unwinds and
      // re-attaches the main-menu root via scene.setRoot.
      Platform.runLater(this::handleAutosavePrompt);
    }
  }

  private void bindLabels() {
    singlePlayerTitle.setText(i18n.t("menu.singleplayer.title"));
    singlePlayerSubtitle.setText(i18n.t("menu.singleplayer.subtitle"));
    singlePlayerButton.setText(i18n.t("menu.open"));

    lanTitle.setText(i18n.t("menu.lan.title"));
    lanSubtitle.setText(i18n.t("menu.lan.subtitle"));
    lanButton.setText(i18n.t("menu.open"));
    lanTooltip.setText(i18n.t("menu.lan.disabled"));

    onlineTitle.setText(i18n.t("menu.online.title"));
    onlineSubtitle.setText(i18n.t("menu.online.subtitle"));
    onlineButton.setText(i18n.t("menu.open"));
    onlineTooltip.setText(i18n.t("menu.online.disabled"));

    rulesTitle.setText(i18n.t("menu.rules.title"));
    rulesSubtitle.setText(i18n.t("menu.rules.subtitle"));
    rulesButton.setText(i18n.t("menu.open"));

    settingsTitle.setText(i18n.t("menu.settings.title"));
    settingsSubtitle.setText(i18n.t("menu.settings.subtitle"));
    settingsButton.setText(i18n.t("menu.open"));
  }

  /** Visible for testing. */
  PromptResult handleAutosavePrompt() {
    boolean resume =
        prompt.confirm(
            "autosave.prompt.title", "autosave.prompt.header", "autosave.prompt.content");
    if (!resume) {
      autosaveService.clearAutosave();
      return PromptResult.DISCARDED;
    }
    Optional<SinglePlayerGame> loaded;
    try {
      loaded = autosaveService.readAutosave();
    } catch (SaveService.UnknownSchemaVersionException ex) {
      log.warn("Autosave has unknown schema {}, discarding", ex.actualVersion());
      prompt.info("autosave.toast.error.schema.title", "autosave.toast.error.schema.content");
      autosaveService.clearAutosave();
      return PromptResult.SCHEMA_MISMATCH;
    } catch (UncheckedIOException ex) {
      log.warn("Autosave could not be read, discarding", ex);
      prompt.info("autosave.toast.error.io.title", "autosave.toast.error.io.content");
      autosaveService.clearAutosave();
      return PromptResult.IO_ERROR;
    }
    if (loaded.isEmpty()) {
      // The file vanished between splash detection and now; nothing to resume.
      return PromptResult.NO_AUTOSAVE;
    }
    gameSession.setCurrentGame(loaded.get());
    sceneRouter.show(SceneId.BOARD);
    return PromptResult.RESUMED;
  }

  @FXML
  void openSinglePlayer() {
    sceneRouter.show(SceneId.SP_SETUP);
  }

  @FXML
  void openRules() {
    sceneRouter.show(SceneId.RULES);
  }

  @FXML
  void openSettings() {
    sceneRouter.show(SceneId.SETTINGS);
  }
}
