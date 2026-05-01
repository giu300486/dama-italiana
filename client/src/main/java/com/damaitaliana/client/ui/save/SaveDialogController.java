package com.damaitaliana.client.ui.save;

import com.damaitaliana.client.app.ThemeService;
import com.damaitaliana.client.app.UserPromptService;
import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.client.persistence.SaveService;
import com.damaitaliana.client.persistence.SavedGame;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Modal "Save as…" dialog. Pre-fills the name field with the live {@link SinglePlayerGame#name()},
 * lets the user edit it, validates non-empty + sluggable, and on confirm asks {@link SaveService}
 * to persist a {@link SavedGame#of(SinglePlayerGame, Instant, Instant)} payload to the
 * corresponding slot. Toasts success / error via {@link UserPromptService#info}.
 *
 * <p>Lifecycle: prototype Spring bean instantiated by the {@link
 * com.damaitaliana.client.ui.board.BoardViewController board view} for each open. {@link
 * #show(SinglePlayerGame, Window)} loads the FXML, attaches this instance as the controller, builds
 * a modal {@link Stage} and blocks on {@code showAndWait}. The boolean return value lets the caller
 * distinguish between "saved" and "cancelled" without exposing internal state.
 */
@Component
@Scope("prototype")
public class SaveDialogController {

  private static final Logger log = LoggerFactory.getLogger(SaveDialogController.class);
  private static final String FXML_PATH = "/fxml/save-dialog.fxml";

  /** Outcome of a confirm attempt; surfaced to {@link #onConfirm()} so the UI can react. */
  enum ConfirmResult {
    SAVED,
    NAME_EMPTY,
    NAME_INVALID,
    OVERWRITE_DECLINED,
    IO_ERROR
  }

  private final SaveService saveService;
  private final UserPromptService prompt;
  private final I18n i18n;
  private final ThemeService themeService;
  private final Clock clock;

  @FXML private Label titleLabel;
  @FXML private Label nameLabel;
  @FXML private TextField nameField;
  @FXML private Label errorLabel;
  @FXML private Button cancelButton;
  @FXML private Button confirmButton;

  private SinglePlayerGame snapshot;
  private Stage stage;
  private boolean saved;

  @Autowired
  public SaveDialogController(
      SaveService saveService, UserPromptService prompt, I18n i18n, ThemeService themeService) {
    this(saveService, prompt, i18n, themeService, Clock.systemUTC());
  }

  /** Visible for tests: lets a fixed clock drive deterministic createdAt/updatedAt timestamps. */
  SaveDialogController(
      SaveService saveService,
      UserPromptService prompt,
      I18n i18n,
      ThemeService themeService,
      Clock clock) {
    this.saveService = Objects.requireNonNull(saveService, "saveService");
    this.prompt = Objects.requireNonNull(prompt, "prompt");
    this.i18n = Objects.requireNonNull(i18n, "i18n");
    this.themeService = Objects.requireNonNull(themeService, "themeService");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Opens the dialog modally, blocks until the user confirms or cancels, and returns whether the
   * save was actually persisted. Loads its own {@link FXMLLoader} bound to {@code this} so the
   * caller doesn't have to plumb a separate setter.
   */
  public boolean show(SinglePlayerGame snapshot, Window owner) {
    this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(FXML_PATH));
      loader.setController(this);
      Parent root = loader.load();
      stage = new Stage();
      stage.setTitle(i18n.t("save.dialog.title"));
      if (owner != null) {
        stage.initOwner(owner);
      }
      stage.initModality(Modality.WINDOW_MODAL);
      Scene scene = new Scene(root);
      themeService.applyTheme(scene);
      stage.setScene(scene);
      stage.setResizable(false);
      stage.showAndWait();
      return saved;
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to load save-dialog.fxml", ex);
    }
  }

  @FXML
  void initialize() {
    titleLabel.setText(i18n.t("save.dialog.title"));
    nameLabel.setText(i18n.t("save.dialog.name.label"));
    cancelButton.setText(i18n.t("common.button.cancel"));
    confirmButton.setText(i18n.t("common.button.save"));
    if (snapshot != null) {
      nameField.setText(snapshot.name());
    }
    errorLabel.setVisible(false);
    errorLabel.setManaged(false);
  }

  @FXML
  void onConfirm() {
    ConfirmResult result = confirm(nameField.getText());
    switch (result) {
      case NAME_EMPTY -> showError(i18n.t("save.dialog.name.empty"));
      case NAME_INVALID -> showError(i18n.t("save.dialog.name.invalid"));
      case SAVED -> closeStage();
      case OVERWRITE_DECLINED, IO_ERROR -> {
        // Leave the dialog open so the user can retry or cancel.
      }
    }
  }

  @FXML
  void onCancel() {
    closeStage();
  }

  /**
   * Visible for tests: business-logic entry point for the confirm action. Pure (no FXML field
   * access) so unit tests can drive it without booting the JavaFX toolkit.
   */
  ConfirmResult confirm(String enteredName) {
    Objects.requireNonNull(snapshot, "snapshot must be set via show() first");
    if (enteredName == null || enteredName.isBlank()) {
      return ConfirmResult.NAME_EMPTY;
    }
    String trimmed = enteredName.strip();
    String slug;
    try {
      slug = SaveService.slugify(trimmed);
    } catch (IllegalArgumentException ex) {
      return ConfirmResult.NAME_INVALID;
    }
    if (slotExists(slug)) {
      boolean overwrite =
          prompt.confirm(
              "save.dialog.confirm.overwrite.title",
              "save.dialog.confirm.overwrite.header",
              "save.dialog.confirm.overwrite.content");
      if (!overwrite) {
        return ConfirmResult.OVERWRITE_DECLINED;
      }
    }
    try {
      Instant now = Instant.now(clock);
      SinglePlayerGame named =
          new SinglePlayerGame(
              snapshot.level(), snapshot.humanColor(), trimmed, snapshot.state(), snapshot.rng());
      saveService.save(slug, SavedGame.of(named, now, now));
      saved = true;
      prompt.info("save.toast.success.title", "save.toast.success.content");
      return ConfirmResult.SAVED;
    } catch (UncheckedIOException ex) {
      log.warn("Save to slot {} failed", slug, ex);
      prompt.info("save.toast.error.title", "save.toast.error.content");
      return ConfirmResult.IO_ERROR;
    }
  }

  /** Visible for tests: lets a test seed the snapshot without going through {@link #show}. */
  void setSnapshotForTest(SinglePlayerGame snapshot) {
    this.snapshot = snapshot;
  }

  /** Visible for tests. */
  boolean wasSaved() {
    return saved;
  }

  private boolean slotExists(String slug) {
    Path savesDir = saveService.savesDir();
    return savesDir != null && Files.exists(savesDir.resolve(slug + ".json"));
  }

  private void showError(String message) {
    errorLabel.setText(message);
    errorLabel.setVisible(true);
    errorLabel.setManaged(true);
  }

  private void closeStage() {
    if (stage != null) {
      stage.close();
    }
  }
}
