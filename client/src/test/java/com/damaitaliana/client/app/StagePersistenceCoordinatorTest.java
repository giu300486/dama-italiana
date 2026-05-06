package com.damaitaliana.client.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.client.persistence.UserPreferences;
import java.util.Locale;
import java.util.Optional;
import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;

/**
 * F4.5 REVIEW F-001 + F-003 + F-004 — pure-math tests for {@link
 * StagePersistenceCoordinator#computePersistedState} covering the persistence-decision flow without
 * the JavaFX toolkit. The branches under test:
 *
 * <ul>
 *   <li><b>Maximized + valid windowed snapshot</b> → save with the windowed geometry and {@code
 *       maximized=true}. Critical fix for F-001 (the previous implementation captured the maximized
 *       geometry as the windowed one).
 *   <li><b>Maximized + null/below-floor snapshot, current already maximized</b> → no-op.
 *   <li><b>Maximized + null/below-floor snapshot, current was windowed</b> → save with {@code
 *       maximized=true} and the previously persisted geometry preserved (fix for F-003).
 *   <li><b>Windowed + valid snapshot</b> → save with the new geometry and {@code maximized=false}.
 *   <li><b>Windowed + null/below-floor snapshot, current was maximized</b> → save flag-only.
 *   <li><b>Windowed + null/below-floor snapshot, current already windowed</b> → no-op (avoids
 *       overwriting valid persisted state with garbage values, fix for F-004).
 * </ul>
 */
class StagePersistenceCoordinatorTest {

  private static final double MIN_WIDTH = 1024.0;
  private static final double MIN_HEIGHT = 720.0;

  @Test
  void maximizedWithValidWindowedSnapshotPersistsWindowedGeometryAndMaximizedTrue() {
    UserPreferences current = preferencesWithGeometry(1366, 768, 100, 50, false);
    Rectangle2D windowedSnapshot = new Rectangle2D(120, 80, 1500, 900);

    Optional<UserPreferences> next =
        StagePersistenceCoordinator.computePersistedState(
            current, windowedSnapshot, true, MIN_WIDTH, MIN_HEIGHT);

    assertThat(next).isPresent();
    UserPreferences saved = next.get();
    assertThat(saved.windowWidth()).isEqualTo(1500);
    assertThat(saved.windowHeight()).isEqualTo(900);
    assertThat(saved.windowX()).isEqualTo(120);
    assertThat(saved.windowY()).isEqualTo(80);
    assertThat(saved.windowMaximized()).isTrue();
  }

  @Test
  void maximizedWithNullSnapshotAndCurrentAlreadyMaximizedIsNoOp() {
    UserPreferences current = preferencesWithGeometry(1366, 768, 100, 50, true);

    Optional<UserPreferences> next =
        StagePersistenceCoordinator.computePersistedState(
            current, null, true, MIN_WIDTH, MIN_HEIGHT);

    assertThat(next).isEmpty();
  }

  @Test
  void maximizedWithNullSnapshotAndCurrentWasWindowedFlipsFlagAndPreservesGeometry() {
    UserPreferences current = preferencesWithGeometry(1366, 768, 100, 50, false);

    Optional<UserPreferences> next =
        StagePersistenceCoordinator.computePersistedState(
            current, null, true, MIN_WIDTH, MIN_HEIGHT);

    assertThat(next).isPresent();
    UserPreferences saved = next.get();
    assertThat(saved.windowWidth()).isEqualTo(1366);
    assertThat(saved.windowHeight()).isEqualTo(768);
    assertThat(saved.windowX()).isEqualTo(100);
    assertThat(saved.windowY()).isEqualTo(50);
    assertThat(saved.windowMaximized()).isTrue();
  }

  @Test
  void windowedWithValidSnapshotPersistsNewGeometryAndMaximizedFalse() {
    UserPreferences current = preferencesWithGeometry(1366, 768, 100, 50, true);
    Rectangle2D windowedSnapshot = new Rectangle2D(200, 150, 1800, 1000);

    Optional<UserPreferences> next =
        StagePersistenceCoordinator.computePersistedState(
            current, windowedSnapshot, false, MIN_WIDTH, MIN_HEIGHT);

    assertThat(next).isPresent();
    UserPreferences saved = next.get();
    assertThat(saved.windowWidth()).isEqualTo(1800);
    assertThat(saved.windowHeight()).isEqualTo(1000);
    assertThat(saved.windowX()).isEqualTo(200);
    assertThat(saved.windowY()).isEqualTo(150);
    assertThat(saved.windowMaximized()).isFalse();
  }

  @Test
  void windowedWithNullSnapshotAndCurrentWasMaximizedFlipsFlagOnly() {
    UserPreferences current = preferencesWithGeometry(1366, 768, 100, 50, true);

    Optional<UserPreferences> next =
        StagePersistenceCoordinator.computePersistedState(
            current, null, false, MIN_WIDTH, MIN_HEIGHT);

    assertThat(next).isPresent();
    assertThat(next.get().windowMaximized()).isFalse();
    assertThat(next.get().windowWidth()).isEqualTo(1366);
    assertThat(next.get().windowHeight()).isEqualTo(768);
  }

  @Test
  void windowedWithNullSnapshotAndCurrentAlreadyWindowedIsNoOp() {
    UserPreferences current = preferencesWithGeometry(1366, 768, 100, 50, false);

    Optional<UserPreferences> next =
        StagePersistenceCoordinator.computePersistedState(
            current, null, false, MIN_WIDTH, MIN_HEIGHT);

    assertThat(next).isEmpty();
  }

  @Test
  void belowFloorSnapshotIsTreatedLikeNull() {
    UserPreferences current = preferencesWithGeometry(1366, 768, 100, 50, false);
    Rectangle2D belowFloor = new Rectangle2D(0, 0, 800, 600);

    Optional<UserPreferences> next =
        StagePersistenceCoordinator.computePersistedState(
            current, belowFloor, false, MIN_WIDTH, MIN_HEIGHT);

    assertThat(next).isEmpty();
  }

  @Test
  void freshInstallScenarioWithNullPersistedGeometryAndMaximizedSnapshotFiresFlagOnlyUpdate() {
    // Fresh install: defaults() has all geometry null + maximized=false.
    UserPreferences current = UserPreferences.defaults();
    Rectangle2D freshSeed = new Rectangle2D(192, 108, 1536, 864);

    Optional<UserPreferences> next =
        StagePersistenceCoordinator.computePersistedState(
            current, freshSeed, true, MIN_WIDTH, MIN_HEIGHT);

    assertThat(next).isPresent();
    UserPreferences saved = next.get();
    assertThat(saved.windowWidth()).isEqualTo(1536);
    assertThat(saved.windowHeight()).isEqualTo(864);
    assertThat(saved.windowMaximized()).isTrue();
  }

  private static UserPreferences preferencesWithGeometry(
      int w, int h, int x, int y, boolean maximized) {
    return new UserPreferences(
        UserPreferences.CURRENT_SCHEMA_VERSION,
        Locale.ITALIAN,
        "light",
        100,
        false,
        UserPreferences.DEFAULT_MUSIC_VOLUME_PERCENT,
        UserPreferences.DEFAULT_SFX_VOLUME_PERCENT,
        false,
        false,
        w,
        h,
        x,
        y,
        maximized);
  }
}
