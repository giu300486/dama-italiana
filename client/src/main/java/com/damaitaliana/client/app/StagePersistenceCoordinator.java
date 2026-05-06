package com.damaitaliana.client.app;

import com.damaitaliana.client.persistence.PreferencesService;
import com.damaitaliana.client.persistence.UserPreferences;
import java.util.Objects;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * F4.5 Task 4.5.7b — orchestrates Stage geometry across launches.
 *
 * <ul>
 *   <li>On startup ({@link #initialize(Stage)}) load the persisted state. If {@link
 *       StagePersistenceValidator} accepts it, restore size/position/maximised state on the primary
 *       stage; otherwise hand off to {@link PrimaryStageInitializer} for the computed 80% baseline.
 *   <li>Wire {@link Stage#setOnCloseRequest} so closing the primary window persists current
 *       geometry into the user preferences file. Re-loads the latest preferences before writing so
 *       concurrent in-session preference changes (audio sliders, locale) are not clobbered.
 * </ul>
 *
 * <p>This bean replaces the direct {@code PrimaryStageInitializer.initialize} call from {@link
 * JavaFxApp#start(Stage)}: when no persisted state is available the behaviour is unchanged (same
 * computed 80% fallback), and when a valid persisted state exists the user sees the window where
 * they left it.
 *
 * <p><b>F4.5 REVIEW fixes</b>:
 *
 * <ul>
 *   <li><b>F-001</b> — the persisted geometry must always be the <em>windowed</em> geometry, never
 *       the maximized one. We continuously track the last known windowed bounds via listeners on
 *       {@link Stage#widthProperty} / heightProperty / xProperty / yProperty (guarded by {@code
 *       !stage.isMaximized()}); on close, if the stage is maximized we read the last windowed
 *       snapshot rather than {@link Stage#getWidth()} / etc. (which would return maximized values).
 *   <li><b>F-001</b> — {@link Stage#setMaximized} is documented as "may be ignored if the stage is
 *       not yet shown"; we defer the call to {@link Platform#runLater} so it lands after {@link
 *       JavaFxApp#start} has invoked {@link Stage#show()}.
 *   <li><b>F-004</b> — if the stage's geometry is unavailable on close (NaN, zero size, or below
 *       the responsive floor), we skip writing the geometry and only update the maximized flag if
 *       it changed; this preserves the previously valid persisted state instead of overwriting it
 *       with garbage values.
 * </ul>
 */
@Component
public class StagePersistenceCoordinator {

  private static final Logger log = LoggerFactory.getLogger(StagePersistenceCoordinator.class);

  private final PreferencesService preferencesService;
  private final PrimaryStageInitializer initializer;

  /**
   * Last known <em>windowed</em> bounds of the stage, updated by the continuous tracking listeners
   * registered in {@link #initialize(Stage)} only while {@code !stage.isMaximized()}. {@code null}
   * until either the stage has been positioned at least once windowed, or the persisted state was
   * restored from a previous session.
   */
  private Rectangle2D lastWindowedBounds;

  public StagePersistenceCoordinator(
      PreferencesService preferencesService, PrimaryStageInitializer initializer) {
    this.preferencesService = Objects.requireNonNull(preferencesService, "preferencesService");
    this.initializer = Objects.requireNonNull(initializer, "initializer");
  }

  public void initialize(Stage stage) {
    Objects.requireNonNull(stage, "stage");

    UserPreferences prefs = preferencesService.load();
    if (StagePersistenceValidator.isStateValidUsingPrimaryScreens(prefs)) {
      log.info(
          "Restoring persisted Stage state {}x{} @ ({}, {}) maximized={}",
          prefs.windowWidth(),
          prefs.windowHeight(),
          prefs.windowX(),
          prefs.windowY(),
          prefs.windowMaximized());
      stage.setMinWidth(PrimaryStageInitializer.MIN_WIDTH);
      stage.setMinHeight(PrimaryStageInitializer.MIN_HEIGHT);
      stage.setX(prefs.windowX());
      stage.setY(prefs.windowY());
      stage.setWidth(prefs.windowWidth());
      stage.setHeight(prefs.windowHeight());
      // Seed the tracker so the first close-from-maximized has a known windowed memory.
      lastWindowedBounds =
          new Rectangle2D(
              prefs.windowX(), prefs.windowY(), prefs.windowWidth(), prefs.windowHeight());
      if (prefs.windowMaximized()) {
        // F-001: defer until after JavaFxApp.start has called stage.show().
        Platform.runLater(() -> stage.setMaximized(true));
      }
    } else {
      initializer.initialize(stage);
    }

    // Seed the tracker with whatever geometry the stage now carries (either the validated
    // persisted bounds or the 80% fallback from PrimaryStageInitializer) so a fresh-install
    // user who maximizes before resizing still has a known windowed memory at close time.
    if (lastWindowedBounds == null) {
      Rectangle2D seed = currentWindowedSnapshot(stage);
      if (seed != null) {
        lastWindowedBounds = seed;
      }
    }

    // F-001: continuously track windowed geometry so persist() has accurate memory even
    // when the stage is closed while maximized.
    ChangeListener<Number> windowedTracker =
        (obs, oldValue, newValue) -> updateWindowedBounds(stage);
    stage.widthProperty().addListener(windowedTracker);
    stage.heightProperty().addListener(windowedTracker);
    stage.xProperty().addListener(windowedTracker);
    stage.yProperty().addListener(windowedTracker);

    stage.setOnCloseRequest(event -> persist(stage));
  }

  private void updateWindowedBounds(Stage stage) {
    if (stage.isMaximized()) {
      return;
    }
    double w = stage.getWidth();
    double h = stage.getHeight();
    double x = stage.getX();
    double y = stage.getY();
    if (Double.isNaN(w) || Double.isNaN(h) || Double.isNaN(x) || Double.isNaN(y)) {
      return;
    }
    if (w <= 0 || h <= 0) {
      return;
    }
    lastWindowedBounds = new Rectangle2D(x, y, w, h);
  }

  private void persist(Stage stage) {
    try {
      UserPreferences current = preferencesService.load();
      boolean isMax = stage.isMaximized();
      Rectangle2D windowedSnapshot = isMax ? lastWindowedBounds : currentWindowedSnapshot(stage);
      Optional<UserPreferences> next =
          computePersistedState(
              current,
              windowedSnapshot,
              isMax,
              PrimaryStageInitializer.MIN_WIDTH,
              PrimaryStageInitializer.MIN_HEIGHT);
      if (next.isEmpty()) {
        log.info(
            "Stage geometry unavailable on close (maximized={}, snapshot={}); preserving previously"
                + " persisted state",
            isMax,
            windowedSnapshot);
        return;
      }
      preferencesService.save(next.get());
    } catch (RuntimeException ex) {
      // Don't block the user from closing the window if persistence fails.
      log.warn("Failed to persist Stage state on close", ex);
    }
  }

  /**
   * F4.5 REVIEW F-001 + F-004 — pure-math decision: given the previously persisted state, the most
   * recent windowed-bounds snapshot (or {@code null} if none was tracked), and whether the stage is
   * currently maximized, returns the {@link UserPreferences} that should be saved or {@link
   * Optional#empty()} if there is nothing meaningful to persist (e.g. windowed snapshot below the
   * responsive floor and the maximized flag is unchanged). Visible for tests.
   */
  static Optional<UserPreferences> computePersistedState(
      UserPreferences current,
      Rectangle2D windowedSnapshot,
      boolean isMaximized,
      double minWidth,
      double minHeight) {
    if (windowedSnapshot == null
        || windowedSnapshot.getWidth() < minWidth
        || windowedSnapshot.getHeight() < minHeight) {
      // No usable geometry snapshot. Preserve the previously persisted geometry; only flip the
      // maximized flag when it actually changed (avoids redundant rewrites of the file).
      if (current.windowMaximized() != isMaximized) {
        return Optional.of(current.withWindowMaximized(isMaximized));
      }
      return Optional.empty();
    }
    return Optional.of(
        current.withWindowState(
            (int) Math.round(windowedSnapshot.getWidth()),
            (int) Math.round(windowedSnapshot.getHeight()),
            (int) Math.round(windowedSnapshot.getMinX()),
            (int) Math.round(windowedSnapshot.getMinY()),
            isMaximized));
  }

  private static Rectangle2D currentWindowedSnapshot(Stage stage) {
    double w = stage.getWidth();
    double h = stage.getHeight();
    double x = stage.getX();
    double y = stage.getY();
    if (Double.isNaN(w) || Double.isNaN(h) || Double.isNaN(x) || Double.isNaN(y)) {
      return null;
    }
    if (w <= 0 || h <= 0) {
      return null;
    }
    return new Rectangle2D(x, y, w, h);
  }
}
