package com.damaitaliana.client.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.client.persistence.UserPreferences;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;

/**
 * F4.5 Task 4.5.7b — pure-math validator tests. No FX toolkit needed: synthetic {@link Rectangle2D}
 * screen bounds are passed in directly, simulating common multi-monitor layouts (single 1920×1080,
 * side-by-side dual, secondary unplugged).
 */
class StagePersistenceValidatorTest {

  private static final List<Rectangle2D> SINGLE_FHD_SCREEN =
      List.of(new Rectangle2D(0, 0, 1920, 1080));

  private static final List<Rectangle2D> DUAL_HORIZONTAL_SCREENS =
      List.of(new Rectangle2D(0, 0, 1920, 1080), new Rectangle2D(1920, 0, 1920, 1080));

  private static final double MIN_WIDTH = 1024;
  private static final double MIN_HEIGHT = 720;

  @Test
  void validStateOnPrimaryScreen() {
    UserPreferences prefs = withWindow(1366, 768, 100, 50, false);
    assertThat(
            StagePersistenceValidator.isStateValid(prefs, SINGLE_FHD_SCREEN, MIN_WIDTH, MIN_HEIGHT))
        .isTrue();
  }

  @Test
  void validStateOnSecondaryScreen() {
    UserPreferences prefs = withWindow(1366, 768, 2200, 50, false);
    assertThat(
            StagePersistenceValidator.isStateValid(
                prefs, DUAL_HORIZONTAL_SCREENS, MIN_WIDTH, MIN_HEIGHT))
        .isTrue();
  }

  @Test
  void invalidWhenAnyGeometryFieldIsNull() {
    UserPreferences prefs =
        new UserPreferences(
            UserPreferences.CURRENT_SCHEMA_VERSION,
            Locale.ITALIAN,
            "light",
            100,
            false,
            UserPreferences.DEFAULT_MUSIC_VOLUME_PERCENT,
            UserPreferences.DEFAULT_SFX_VOLUME_PERCENT,
            false,
            false,
            null,
            null,
            null,
            null,
            false);
    assertThat(
            StagePersistenceValidator.isStateValid(prefs, SINGLE_FHD_SCREEN, MIN_WIDTH, MIN_HEIGHT))
        .isFalse();
  }

  @Test
  void invalidWhenWidthBelowFloor() {
    UserPreferences prefs = withWindow(800, 720, 100, 50, false);
    assertThat(
            StagePersistenceValidator.isStateValid(prefs, SINGLE_FHD_SCREEN, MIN_WIDTH, MIN_HEIGHT))
        .isFalse();
  }

  @Test
  void invalidWhenHeightBelowFloor() {
    UserPreferences prefs = withWindow(1366, 600, 100, 50, false);
    assertThat(
            StagePersistenceValidator.isStateValid(prefs, SINGLE_FHD_SCREEN, MIN_WIDTH, MIN_HEIGHT))
        .isFalse();
  }

  @Test
  void invalidWhenTopLeftCornerOffscreenSecondaryUnplugged() {
    // Window remembers it was on secondary monitor at X=2200, but only the primary is connected.
    UserPreferences prefs = withWindow(1366, 768, 2200, 50, false);
    assertThat(
            StagePersistenceValidator.isStateValid(prefs, SINGLE_FHD_SCREEN, MIN_WIDTH, MIN_HEIGHT))
        .isFalse();
  }

  @Test
  void invalidWhenNullPrefs() {
    assertThat(
            StagePersistenceValidator.isStateValid(null, SINGLE_FHD_SCREEN, MIN_WIDTH, MIN_HEIGHT))
        .isFalse();
  }

  @Test
  void boundaryAtFloorIsValid() {
    UserPreferences prefs = withWindow((int) MIN_WIDTH, (int) MIN_HEIGHT, 0, 0, false);
    assertThat(
            StagePersistenceValidator.isStateValid(prefs, SINGLE_FHD_SCREEN, MIN_WIDTH, MIN_HEIGHT))
        .isTrue();
  }

  @Test
  void maximizedFlagDoesNotAffectValidity() {
    UserPreferences prefs = withWindow(1366, 768, 100, 50, true);
    assertThat(
            StagePersistenceValidator.isStateValid(prefs, SINGLE_FHD_SCREEN, MIN_WIDTH, MIN_HEIGHT))
        .isTrue();
  }

  private static UserPreferences withWindow(int w, int h, int x, int y, boolean maximized) {
    return UserPreferences.defaults().withWindowState(w, h, x, y, maximized);
  }
}
