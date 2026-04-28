package com.damaitaliana.client.ui.board;

import com.damaitaliana.client.app.SceneId;
import com.damaitaliana.client.app.SceneRouter;
import com.damaitaliana.client.controller.GameSession;
import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.client.i18n.I18n;
import java.util.Objects;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Controller backing {@code board-view.fxml}. In Task 3.8 it does the minimum needed to validate
 * the rendering pipeline end-to-end:
 *
 * <ul>
 *   <li>Pulls the current {@link SinglePlayerGame} from {@link GameSession}.
 *   <li>Asks {@link BoardRenderer} to paint the initial position.
 *   <li>Shows the game name and a placeholder side panel that Task 3.11/3.12 will populate with
 *       move history and status.
 * </ul>
 *
 * <p>The actual move-handling loop (click selection, AI scheduling, autosave hooks) lands in Task
 * 3.9 / 3.13 / 3.16.
 */
@Component
@Scope("prototype")
public class BoardViewController {

  private final SceneRouter sceneRouter;
  private final GameSession gameSession;
  private final I18n i18n;

  @FXML private BoardRenderer boardRenderer;
  @FXML private Label gameTitleLabel;
  @FXML private Label sidePanelPlaceholderLabel;
  @FXML private Button backButton;

  public BoardViewController(SceneRouter sceneRouter, GameSession gameSession, I18n i18n) {
    this.sceneRouter = Objects.requireNonNull(sceneRouter, "sceneRouter");
    this.gameSession = Objects.requireNonNull(gameSession, "gameSession");
    this.i18n = Objects.requireNonNull(i18n, "i18n");
  }

  @FXML
  void initialize() {
    Optional<SinglePlayerGame> currentGame = gameSession.currentGame();
    if (currentGame.isEmpty()) {
      sceneRouter.show(SceneId.MAIN_MENU);
      return;
    }
    SinglePlayerGame game = currentGame.get();
    gameTitleLabel.setText(game.name());
    sidePanelPlaceholderLabel.setText(i18n.t("board.sidepanel.placeholder"));
    backButton.setText(i18n.t("common.button.back"));
    boardRenderer.renderState(game.state().board());
  }

  @FXML
  void onBack() {
    sceneRouter.show(SceneId.MAIN_MENU);
  }
}
