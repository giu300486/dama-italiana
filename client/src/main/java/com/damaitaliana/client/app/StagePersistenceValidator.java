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
 *   <li>The persisted window rectangle does not overlap the union of {@link
 *       Screen#getVisualBounds() screen visualBounds} by at least {@link #MIN_INTERSECTION_RATIO} —
 *       happens when the user unplugs the secondary monitor on which the window was last shown, or
 *       moves it so that most of the chrome ends up off-screen. The previous implementation only
 *       checked that the top-left corner lay inside <em>some</em> screen, which let through windows
 *       whose right/bottom edges fell off all displays (F4.5 REVIEW F-002).
 * </ul>
 *
 * <p>The pure-math {@link #isStateValid(UserPreferences, List, double, double)} takes synthetic
 * bounds so unit tests can simulate multi-monitor layouts without booting the JavaFX toolkit. The
 * production wrapper {@link #isStateValidUsingPrimaryScreens(UserPreferences)} pulls the live
 * screen list and the {@code PrimaryStageInitializer} min size for the JavaFx app.
 */
public final class StagePersistenceValidator {

  /**
   * F4.5 REVIEW F-002 — minimum fraction of the persisted window rectangle that must lie within the
   * union of the current screens' visualBounds for the state to be considered safe to restore. 50%
   * gives the user enough chrome to drag the window to a usable position without auto-hiding the
   * title bar.
   */
  static final double MIN_INTERSECTION_RATIO = 0.5;

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
    Rectangle2D window =
        new Rectangle2D(
            prefs.windowX(), prefs.windowY(), prefs.windowWidth(), prefs.windowHeight());
    double windowArea = window.getWidth() * window.getHeight();
    double bestRatio = 0.0;
    for (Rectangle2D bounds : screenVisualBounds) {
      double interW =
          Math.min(bounds.getMaxX(), window.getMaxX())
              - Math.max(bounds.getMinX(), window.getMinX());
      double interH =
          Math.min(bounds.getMaxY(), window.getMaxY())
              - Math.max(bounds.getMinY(), window.getMinY());
      if (interW <= 0 || interH <= 0) {
        continue;
      }
      double ratio = (interW * interH) / windowArea;
      if (ratio > bestRatio) {
        bestRatio = ratio;
      }
    }
    return bestRatio >= MIN_INTERSECTION_RATIO;
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
