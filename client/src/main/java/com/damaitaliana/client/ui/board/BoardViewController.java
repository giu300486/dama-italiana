package com.damaitaliana.client.ui.board;

import com.damaitaliana.client.app.SceneId;
import com.damaitaliana.client.app.SceneRouter;
import com.damaitaliana.client.app.UserPromptService;
import com.damaitaliana.client.controller.GameSession;
import com.damaitaliana.client.controller.SinglePlayerController;
import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.client.persistence.AutosaveService;
import com.damaitaliana.client.ui.save.SaveDialogController;
import java.util.Objects;
import java.util.Optional;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.Window;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Controller backing {@code board-view.fxml}. Pulls the current {@link SinglePlayerGame} from
 * {@link GameSession}, then hands it to a fresh {@link SinglePlayerController} prototype that owns
 * the click protocol and game-state mutations. Falls back to MAIN_MENU when navigated without a
 * configured game.
 *
 * <p>Top-bar menu (Task 3.15): the {@code Partita} menu exposes <em>Salva con nome…</em> (opens the
 * {@link SaveDialogController save dialog} pre-filled with the current snapshot), <em>Carica</em>
 * (navigates to the load screen, stopping the live AI), and <em>Termina partita</em> (confirms,
 * clears the autosave via {@link AutosaveService}, drops the session and returns to the main menu).
 */
@Component
@Scope("prototype")
public class BoardViewController {

  private final SceneRouter sceneRouter;
  private final GameSession gameSession;
  private final I18n i18n;
  private final AutosaveService autosaveService;
  private final UserPromptService prompt;
  private final ObjectProvider<SinglePlayerController> singlePlayerControllerProvider;
  private final ObjectProvider<SaveDialogController> saveDialogControllerProvider;

  @FXML private MenuBar menuBar;
  @FXML private Menu gameMenu;
  @FXML private MenuItem saveMenuItem;
  @FXML private MenuItem loadMenuItem;
  @FXML private MenuItem rulesMenuItem;
  @FXML private MenuItem terminateMenuItem;
  @FXML private BoardRenderer boardRenderer;
  @FXML private Label gameTitleLabel;
  @FXML private StatusPane statusPane;
  @FXML private MoveHistoryView moveHistoryView;
  @FXML private Button backButton;

  private SinglePlayerController gameController;

  public BoardViewController(
      SceneRouter sceneRouter,
      GameSession gameSession,
      I18n i18n,
      AutosaveService autosaveService,
      UserPromptService prompt,
      ObjectProvider<SinglePlayerController> singlePlayerControllerProvider,
      ObjectProvider<SaveDialogController> saveDialogControllerProvider) {
    this.sceneRouter = Objects.requireNonNull(sceneRouter, "sceneRouter");
    this.gameSession = Objects.requireNonNull(gameSession, "gameSession");
    this.i18n = Objects.requireNonNull(i18n, "i18n");
    this.autosaveService = Objects.requireNonNull(autosaveService, "autosaveService");
    this.prompt = Objects.requireNonNull(prompt, "prompt");
    this.singlePlayerControllerProvider =
        Objects.requireNonNull(singlePlayerControllerProvider, "singlePlayerControllerProvider");
    this.saveDialogControllerProvider =
        Objects.requireNonNull(saveDialogControllerProvider, "saveDialogControllerProvider");
  }

  @FXML
  void initialize() {
    bindMenuLabels();
    Optional<SinglePlayerGame> currentGame = gameSession.currentGame();
    if (currentGame.isEmpty()) {
      // Defer past the current SceneRouter.show() call: a synchronous show(MAIN_MENU)
      // here would be undone when the outer show(BOARD) unwinds and re-attaches the
      // (empty) board root via scene.setRoot.
      Platform.runLater(() -> sceneRouter.show(SceneId.MAIN_MENU));
      return;
    }
    SinglePlayerGame game = currentGame.get();
    gameTitleLabel.setText(game.name());
    backButton.setText(i18n.t("common.button.back"));

    StatusPaneViewModel statusViewModel = new StatusPaneViewModel(i18n);
    gameController = singlePlayerControllerProvider.getObject();
    gameController.setStateChangeListener(
        state -> statusPane.update(statusViewModel.compute(game, state)));
    gameController
        .aiThinkingState()
        .onChange(thinking -> statusPane.setThinking(thinking, i18n.t("status.thinking")));
    gameController.start(game, boardRenderer);
    moveHistoryView.setItems(gameController.history().rows());
  }

  private void bindMenuLabels() {
    gameMenu.setText(i18n.t("board.menu.game"));
    saveMenuItem.setText(i18n.t("board.menu.save"));
    loadMenuItem.setText(i18n.t("board.menu.load"));
    rulesMenuItem.setText(i18n.t("board.menu.rules"));
    terminateMenuItem.setText(i18n.t("board.menu.terminate"));
  }

  @FXML
  void onBack() {
    if (gameController != null) {
      gameController.stop();
    }
    sceneRouter.show(SceneId.MAIN_MENU);
  }

  @FXML
  void onSaveAs() {
    openSaveDialog();
  }

  @FXML
  void onLoad() {
    if (gameController != null) {
      gameController.stop();
    }
    sceneRouter.show(SceneId.LOAD);
  }

  @FXML
  void onTerminate() {
    terminate();
  }

  @FXML
  void onShowRules() {
    if (gameController != null) {
      gameController.stop();
    }
    sceneRouter.show(SceneId.RULES);
  }

  /** Visible for tests: business logic for "Salva con nome" without FXML node access. */
  boolean openSaveDialog() {
    if (gameController == null) {
      return false;
    }
    SinglePlayerGame snapshot = gameController.currentSnapshot();
    SaveDialogController dialog = saveDialogControllerProvider.getObject();
    return dialog.show(snapshot, ownerWindow());
  }

  /** Visible for tests: business logic for "Termina partita". */
  boolean terminate() {
    boolean ok =
        prompt.confirm(
            "board.terminate.title", "board.terminate.header", "board.terminate.content");
    if (!ok) {
      return false;
    }
    if (gameController != null) {
      gameController.stop();
    }
    autosaveService.clearAutosave();
    gameSession.clear();
    sceneRouter.show(SceneId.MAIN_MENU);
    return true;
  }

  /** Visible for tests: lets a stub controller replace the live one. */
  void setGameControllerForTest(SinglePlayerController controller) {
    this.gameController = controller;
  }

  private Window ownerWindow() {
    if (gameTitleLabel == null || gameTitleLabel.getScene() == null) {
      return null;
    }
    return gameTitleLabel.getScene().getWindow();
  }
}
