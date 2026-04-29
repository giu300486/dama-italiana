package com.damaitaliana.client.ui.save;

import com.damaitaliana.client.persistence.SerializedGameState;
import javafx.scene.image.Image;

/**
 * Renders a small thumbnail of a saved board position for the load-screen table (Task 3.15).
 *
 * <p>Pulled behind an interface so unit tests of the {@link LoadScreenController} can drop in a
 * stub returning {@code null} without booting the JavaFX toolkit; production wiring uses the
 * Canvas-backed implementation.
 */
@FunctionalInterface
public interface MiniatureRenderer {

  /**
   * Returns a square thumbnail of {@code state} of the requested side length in pixels, or {@code
   * null} if rendering is not possible (e.g. invalid state, headless environment).
   */
  Image render(SerializedGameState state, int sizePx);
}
