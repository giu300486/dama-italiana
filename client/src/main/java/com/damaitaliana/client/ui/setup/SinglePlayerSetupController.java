package com.damaitaliana.client.ui.setup;

import com.damaitaliana.client.app.SceneId;
import com.damaitaliana.client.app.SceneRouter;
import com.damaitaliana.client.controller.ColorChoice;
import com.damaitaliana.client.controller.GameSession;
import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.client.i18n.LocaleService;
import com.damaitaliana.shared.ai.AiLevel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Controller backing {@code sp-setup.fxml}. Defaults: Esperto + White + a date-stamped game name.
 * On confirm builds a {@link SinglePlayerGame} (validated via {@link
 * SinglePlayerGame#tryCreate(AiLevel, ColorChoice, String, RandomGenerator)}), publishes it on
 * {@link GameSession} and asks {@link SceneRouter} to show the board.
 */
@Component
@Scope("prototype")
public class SinglePlayerSetupController {

  private static final DateTimeFormatter NAME_DATE_FORMAT =
      DateTimeFormatter.ofPattern("dd MMM HH:mm");

  private final SceneRouter sceneRouter;
  private final I18n i18n;
  private final LocaleService localeService;
  private final GameSession gameSession;

  @FXML private Label titleLabel;

  @FXML private Label levelLabel;
  @FXML private ToggleGroup levelGroup;
  @FXML private RadioButton levelPrincipiante;
  @FXML private RadioButton levelEsperto;
  @FXML private RadioButton levelCampione;

  @FXML private Label colorLabel;
  @FXML private ToggleGroup colorGroup;
  @FXML private RadioButton colorWhite;
  @FXML private RadioButton colorBlack;
  @FXML private RadioButton colorRandom;

  @FXML private Label nameLabel;
  @FXML private TextField nameField;
  @FXML private Label errorLabel;

  @FXML private Button cancelButton;
  @FXML private Button confirmButton;

  public SinglePlayerSetupController(
      SceneRouter sceneRouter, I18n i18n, LocaleService localeService, GameSession gameSession) {
    this.sceneRouter = Objects.requireNonNull(sceneRouter, "sceneRouter");
    this.i18n = Objects.requireNonNull(i18n, "i18n");
    this.localeService = Objects.requireNonNull(localeService, "localeService");
    this.gameSession = Objects.requireNonNull(gameSession, "gameSession");
  }

  @FXML
  void initialize() {
    bindLabels();
    levelEsperto.setSelected(true);
    colorWhite.setSelected(true);
    nameField.setText(defaultGameName());
    errorLabel.setVisible(false);
  }

  private void bindLabels() {
    titleLabel.setText(i18n.t("setup.title"));
    levelLabel.setText(i18n.t("setup.level.label"));
    levelPrincipiante.setText(i18n.t("setup.level.principiante"));
    levelEsperto.setText(i18n.t("setup.level.esperto"));
    levelCampione.setText(i18n.t("setup.level.campione"));
    colorLabel.setText(i18n.t("setup.color.label"));
    colorWhite.setText(i18n.t("setup.color.white"));
    colorBlack.setText(i18n.t("setup.color.black"));
    colorRandom.setText(i18n.t("setup.color.random"));
    nameLabel.setText(i18n.t("setup.name.label"));
    cancelButton.setText(i18n.t("common.button.cancel"));
    confirmButton.setText(i18n.t("setup.button.confirm"));
  }

  private String defaultGameName() {
    String formatted =
        LocalDateTime.now().format(NAME_DATE_FORMAT.localizedBy(localeService.current()));
    return i18n.t("setup.name.default", formatted);
  }

  @FXML
  void onConfirm() {
    Optional<SinglePlayerGame> game =
        SinglePlayerGame.tryCreate(
            selectedLevel(), selectedColorChoice(), nameField.getText(), newRng());
    if (game.isEmpty()) {
      errorLabel.setText(i18n.t("setup.error.name.empty"));
      errorLabel.setVisible(true);
      return;
    }
    gameSession.setCurrentGame(game.get());
    sceneRouter.show(SceneId.BOARD);
  }

  @FXML
  void onCancel() {
    sceneRouter.show(SceneId.MAIN_MENU);
  }

  private AiLevel selectedLevel() {
    if (levelPrincipiante.isSelected()) {
      return AiLevel.PRINCIPIANTE;
    }
    if (levelCampione.isSelected()) {
      return AiLevel.CAMPIONE;
    }
    return AiLevel.ESPERTO;
  }

  private ColorChoice selectedColorChoice() {
    if (colorBlack.isSelected()) {
      return ColorChoice.BLACK;
    }
    if (colorRandom.isSelected()) {
      return ColorChoice.RANDOM;
    }
    return ColorChoice.WHITE;
  }

  RandomGenerator newRng() {
    return new SplittableRandom(System.nanoTime());
  }
}
