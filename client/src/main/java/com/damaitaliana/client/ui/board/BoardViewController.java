package com.damaitaliana.client.ui.board;

import com.damaitaliana.client.app.SceneId;
import com.damaitaliana.client.app.SceneRouter;
import com.damaitaliana.client.controller.GameSession;
import com.damaitaliana.client.controller.SinglePlayerController;
import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.client.i18n.I18n;
import java.util.Objects;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Controller backing {@code board-view.fxml}. Pulls the current {@link SinglePlayerGame} from
 * {@link GameSession}, then hands it to a fresh {@link SinglePlayerController} prototype that owns
 * the click protocol and game-state mutations. Falls back to MAIN_MENU when navigated without a
 * configured game.
 */
@Component
@Scope("prototype")
public class BoardViewController {

  private final SceneRouter sceneRouter;
  private final GameSession gameSession;
  private final I18n i18n;
  private final ObjectProvider<SinglePlayerController> singlePlayerControllerProvider;

  @FXML private BoardRenderer boardRenderer;
  @FXML private Label gameTitleLabel;
  @FXML private MoveHistoryView moveHistoryView;
  @FXML private Button backButton;

  public BoardViewController(
      SceneRouter sceneRouter,
      GameSession gameSession,
      I18n i18n,
      ObjectProvider<SinglePlayerController> singlePlayerControllerProvider) {
    this.sceneRouter = Objects.requireNonNull(sceneRouter, "sceneRouter");
    this.gameSession = Objects.requireNonNull(gameSession, "gameSession");
    this.i18n = Objects.requireNonNull(i18n, "i18n");
    this.singlePlayerControllerProvider =
        Objects.requireNonNull(singlePlayerControllerProvider, "singlePlayerControllerProvider");
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
    backButton.setText(i18n.t("common.button.back"));

    SinglePlayerController gameController = singlePlayerControllerProvider.getObject();
    gameController.start(game, boardRenderer);
    moveHistoryView.setItems(gameController.history().rows());
  }

  @FXML
  void onBack() {
    sceneRouter.show(SceneId.MAIN_MENU);
  }
}
