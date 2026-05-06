package com.damaitaliana.client.app;

import java.util.Objects;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

/**
 * Configures the primary {@link Stage} according to the F4.5 responsive baseline (PLAN-fase-4.5
 * Task 4.5.3 + ADR-043 + SPEC §13.7):
 *
 * <ul>
 *   <li>Minimum size {@value #MIN_WIDTH}&times;{@value #MIN_HEIGHT} logical pixels — the smallest
 *       desktop viewport the design system still renders without clipping. Below this the side
 *       panel auto-hides and the board collapses, so we hard-stop the user resize there.
 *   <li>Initial size is {@value #INITIAL_SIZE_RATIO_PERCENT}% of the primary screen's {@link
 *       Screen#getVisualBounds() visualBounds} (which already excludes the OS taskbar), clamped to
 *       the minimum on small displays. The window is centered on the primary screen.
 * </ul>
 *
 * <p>Stage state persistence (window position, size and maximised flag from the previous launch) is
 * wired in Task 4.5.7b via {@code PreferencesService} schema v3. When a persisted state exists, the
 * caller restores it instead of using the computed 80% default.
 *
 * <p>Splash and modal dialogs are unaffected: splash runs on the primary stage but is transient
 * (~1.5s, not user-resizable in practice), and the save dialog uses its own modal stage with {@code
 * setResizable(false)} so it does not inherit the constraints.
 */
@Component
public class PrimaryStageInitializer {

  public static final double MIN_WIDTH = 1024;
  public static final double MIN_HEIGHT = 720;
  static final int INITIAL_SIZE_RATIO_PERCENT = 80;
  private static final double INITIAL_SIZE_RATIO = INITIAL_SIZE_RATIO_PERCENT / 100.0;

  public void initialize(Stage stage) {
    Objects.requireNonNull(stage, "stage");
    stage.setMinWidth(MIN_WIDTH);
    stage.setMinHeight(MIN_HEIGHT);

    Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
    double width = Math.max(MIN_WIDTH, bounds.getWidth() * INITIAL_SIZE_RATIO);
    double height = Math.max(MIN_HEIGHT, bounds.getHeight() * INITIAL_SIZE_RATIO);
    stage.setWidth(width);
    stage.setHeight(height);
    stage.setX(bounds.getMinX() + (bounds.getWidth() - width) / 2);
    stage.setY(bounds.getMinY() + (bounds.getHeight() - height) / 2);
  }
}
