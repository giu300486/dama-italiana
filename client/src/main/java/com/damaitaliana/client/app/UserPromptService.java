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
   * <p>{@code formatArgs} are MessageFormat-style placeholders applied to all three keys (a key
   * without placeholders simply ignores them).
   *
   * @return {@code true} if the user accepted, {@code false} if they declined or closed the dialog.
   */
  boolean confirm(String titleKey, String headerKey, String contentKey, Object... formatArgs);

  /** No-argument convenience overload for the common case. */
  default boolean confirm(String titleKey, String headerKey, String contentKey) {
    return confirm(titleKey, headerKey, contentKey, (Object[]) null);
  }

  /**
   * Shows a blocking informational dialog with localized title/content. Used for transient "save
   * successful" / "save failed" toasts (Task 3.15). {@code contentArgs} are {@link
   * java.text.MessageFormat MessageFormat}-style placeholders for the content message.
   */
  void info(String titleKey, String contentKey, Object... contentArgs);
}
