package com.damaitaliana.client.app;

/**
 * Yes/no dialog abstraction. Decouples controllers from {@link javafx.scene.control.Alert} so unit
 * tests can drive the prompt result without booting the JavaFX toolkit.
 *
 * <p>Production wiring: {@link JavaFxUserPromptService}.
 */
public interface UserPromptService {

  /**
   * Shows a confirmation dialog with localized title/header/content.
   *
   * @return {@code true} if the user accepted, {@code false} if they declined or closed the dialog.
   */
  boolean confirm(String titleKey, String headerKey, String contentKey);
}
