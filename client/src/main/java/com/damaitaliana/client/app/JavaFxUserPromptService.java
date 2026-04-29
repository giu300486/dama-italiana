package com.damaitaliana.client.app;

import com.damaitaliana.client.i18n.I18n;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import org.springframework.stereotype.Component;

/**
 * Default JavaFX-backed implementation of {@link UserPromptService}. Constructs a {@code
 * CONFIRMATION} {@link Alert} with localized strings and blocks on {@code showAndWait}.
 */
@Component
public class JavaFxUserPromptService implements UserPromptService {

  private final I18n i18n;

  public JavaFxUserPromptService(I18n i18n) {
    this.i18n = i18n;
  }

  @Override
  public boolean confirm(
      String titleKey, String headerKey, String contentKey, Object... formatArgs) {
    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setTitle(i18n.t(titleKey, formatArgs));
    alert.setHeaderText(i18n.t(headerKey, formatArgs));
    alert.setContentText(i18n.t(contentKey, formatArgs));
    return alert.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
  }

  @Override
  public void info(String titleKey, String contentKey, Object... contentArgs) {
    Alert alert = new Alert(AlertType.INFORMATION);
    alert.setTitle(i18n.t(titleKey));
    alert.setHeaderText(null);
    alert.setContentText(i18n.t(contentKey, contentArgs));
    alert.showAndWait();
  }
}
