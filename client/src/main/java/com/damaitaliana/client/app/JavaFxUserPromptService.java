package com.damaitaliana.client.app;

import com.damaitaliana.client.i18n.I18n;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.stereotype.Component;

/**
 * Default JavaFX-backed implementation of {@link UserPromptService}.
 *
 * <p>Wood-premium custom dialog (post-3.5.11 visual review): instead of the native {@link
 * javafx.scene.control.Alert} — whose stage chrome is owned by the platform and clashes with the
 * wood theme — this implementation builds a custom {@link Stage} with {@link
 * StageStyle#UNDECORATED} so the entire surface is themable. The dialog has a draggable deep-walnut
 * header bar with a gold title + close glyph, a cream content area, and a button bar themed with
 * {@code .button-primary} / {@code .button-secondary}. Stylesheets are pulled from {@link
 * ThemeService} so the dialog inherits the same active theme as the rest of the app.
 */
@Component
public class JavaFxUserPromptService implements UserPromptService {

  private final I18n i18n;
  private final ThemeService themeService;

  public JavaFxUserPromptService(I18n i18n, ThemeService themeService) {
    this.i18n = i18n;
    this.themeService = themeService;
  }

  @Override
  public boolean confirm(
      String titleKey, String headerKey, String contentKey, Object... formatArgs) {
    AtomicBoolean accepted = new AtomicBoolean(false);
    Stage stage =
        buildDialog(
            i18n.t(titleKey, formatArgs),
            i18n.t(headerKey, formatArgs),
            i18n.t(contentKey, formatArgs),
            true,
            () -> accepted.set(true));
    stage.showAndWait();
    return accepted.get();
  }

  @Override
  public void info(String titleKey, String contentKey, Object... contentArgs) {
    Stage stage = buildDialog(i18n.t(titleKey), null, i18n.t(contentKey, contentArgs), false, null);
    stage.showAndWait();
  }

  private Stage buildDialog(
      String title, String header, String content, boolean isConfirm, Runnable onAccept) {
    Stage stage = new Stage(StageStyle.UNDECORATED);
    stage.initModality(Modality.APPLICATION_MODAL);
    stage.setResizable(false);

    Label titleLabel = new Label(title);
    titleLabel.getStyleClass().addAll("label-display", "themed-dialog-title");
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    Button closeButton = new Button("✕"); // ✕
    closeButton.getStyleClass().add("themed-dialog-close");
    closeButton.setFocusTraversable(false);
    closeButton.setOnAction(e -> stage.close());

    HBox headerBar = new HBox(12, titleLabel, spacer, closeButton);
    headerBar.getStyleClass().add("themed-dialog-header");
    headerBar.setAlignment(Pos.CENTER_LEFT);
    headerBar.setPadding(new Insets(10, 12, 10, 18));
    final double[] dragOffset = new double[2];
    headerBar.setOnMousePressed(
        e -> {
          dragOffset[0] = e.getScreenX() - stage.getX();
          dragOffset[1] = e.getScreenY() - stage.getY();
        });
    headerBar.setOnMouseDragged(
        e -> {
          stage.setX(e.getScreenX() - dragOffset[0]);
          stage.setY(e.getScreenY() - dragOffset[1]);
        });

    VBox contentBox = new VBox(10);
    contentBox.getStyleClass().add("themed-dialog-content");
    contentBox.setPadding(new Insets(20, 24, 20, 24));
    if (header != null && !header.isBlank()) {
      Label headerLabel = new Label(header);
      headerLabel.getStyleClass().add("themed-dialog-header-text");
      headerLabel.setWrapText(true);
      contentBox.getChildren().add(headerLabel);
    }
    Label contentLabel = new Label(content);
    contentLabel.setWrapText(true);
    contentBox.getChildren().add(contentLabel);
    VBox.setVgrow(contentBox, Priority.ALWAYS);

    HBox buttonBar = new HBox(12);
    buttonBar.getStyleClass().add("themed-dialog-button-bar");
    buttonBar.setAlignment(Pos.CENTER_RIGHT);
    buttonBar.setPadding(new Insets(12, 24, 18, 24));
    if (isConfirm) {
      Button cancel = new Button(i18n.t("common.button.cancel"));
      cancel.getStyleClass().add("button-secondary");
      cancel.setCancelButton(true);
      cancel.setOnAction(e -> stage.close());
      Button ok = new Button(i18n.t("common.button.confirm"));
      ok.getStyleClass().add("button-primary");
      ok.setDefaultButton(true);
      ok.setOnAction(
          e -> {
            if (onAccept != null) {
              onAccept.run();
            }
            stage.close();
          });
      buttonBar.getChildren().addAll(cancel, ok);
      stage.setOnShown(e -> ok.requestFocus());
    } else {
      Button ok = new Button(i18n.t("common.button.ok"));
      ok.getStyleClass().add("button-primary");
      ok.setDefaultButton(true);
      ok.setCancelButton(true);
      ok.setOnAction(e -> stage.close());
      buttonBar.getChildren().add(ok);
      stage.setOnShown(e -> ok.requestFocus());
    }

    VBox root = new VBox(headerBar, contentBox, buttonBar);
    root.getStyleClass().add("themed-dialog");
    root.setMinWidth(380);
    root.setMaxWidth(560);

    Scene scene = new Scene(root);
    themeService.applyTheme(scene);
    stage.setScene(scene);
    stage.sizeToScene();
    return stage;
  }
}
