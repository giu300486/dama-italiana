package com.damaitaliana.client.app;

import com.damaitaliana.client.persistence.PreferencesService;
import com.damaitaliana.client.persistence.UserPreferences;
import java.util.Objects;
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
 */
@Component
public class StagePersistenceCoordinator {

  private static final Logger log = LoggerFactory.getLogger(StagePersistenceCoordinator.class);

  private final PreferencesService preferencesService;
  private final PrimaryStageInitializer initializer;

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
      stage.setMaximized(prefs.windowMaximized());
    } else {
      initializer.initialize(stage);
    }

    stage.setOnCloseRequest(event -> persist(stage));
  }

  private void persist(Stage stage) {
    try {
      UserPreferences current = preferencesService.load();
      UserPreferences updated =
          current.withWindowState(
              (int) Math.round(stage.getWidth()),
              (int) Math.round(stage.getHeight()),
              (int) Math.round(stage.getX()),
              (int) Math.round(stage.getY()),
              stage.isMaximized());
      preferencesService.save(updated);
    } catch (RuntimeException ex) {
      // Don't block the user from closing the window if persistence fails.
      log.warn("Failed to persist Stage state on close", ex);
    }
  }
}
