package com.damaitaliana.client.app;

import com.damaitaliana.client.persistence.UserPreferences;
import java.util.List;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

/**
 * F4.5 Task 4.5.7b — decides whether a previously persisted Stage state ({@link UserPreferences}
 * window fields) is safe to restore on the current display set, or whether we should fall back to
 * the computed 80% baseline of {@link PrimaryStageInitializer}.
 *
 * <p>Failure cases the validator catches:
 *
 * <ul>
 *   <li>Any of the four geometry fields is {@code null} (fresh install, v2 file just migrated).
 *   <li>Width or height below the responsive floor 1024×720 (corrupted file, schema downgrade).
 *   <li>Top-left corner sits outside every screen's {@link Screen#getVisualBounds() visualBounds} —
 *       happens when the user unplugs the secondary monitor on which the window was last shown.
 * </ul>
 *
 * <p>The pure-math {@link #isStateValid(UserPreferences, List, double, double)} takes synthetic
 * bounds so unit tests can simulate multi-monitor layouts without booting the JavaFX toolkit. The
 * production wrapper {@link #isStateValidUsingPrimaryScreens(UserPreferences)} pulls the live
 * screen list and the {@code PrimaryStageInitializer} min size for the JavaFx app.
 */
public final class StagePersistenceValidator {

  private StagePersistenceValidator() {}

  public static boolean isStateValid(
      UserPreferences prefs,
      List<Rectangle2D> screenVisualBounds,
      double minWidth,
      double minHeight) {
    if (prefs == null
        || prefs.windowWidth() == null
        || prefs.windowHeight() == null
        || prefs.windowX() == null
        || prefs.windowY() == null) {
      return false;
    }
    if (prefs.windowWidth() < minWidth || prefs.windowHeight() < minHeight) {
      return false;
    }
    double x = prefs.windowX();
    double y = prefs.windowY();
    for (Rectangle2D bounds : screenVisualBounds) {
      if (bounds.contains(x, y)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Production wrapper: queries the live {@link Screen} list and the {@link
   * PrimaryStageInitializer} min size. Must be called on the JavaFX thread.
   */
  public static boolean isStateValidUsingPrimaryScreens(UserPreferences prefs) {
    List<Rectangle2D> bounds = Screen.getScreens().stream().map(Screen::getVisualBounds).toList();
    return isStateValid(
        prefs, bounds, PrimaryStageInitializer.MIN_WIDTH, PrimaryStageInitializer.MIN_HEIGHT);
  }
}
