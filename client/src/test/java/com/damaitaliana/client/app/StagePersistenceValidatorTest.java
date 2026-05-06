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
  void invalidWhenTopLeftIsOnscreenButMostOfWindowIsOffscreen() {
    // F4.5 REVIEW F-002: window top-left at (1900, 50) is inside the 1920x1080 primary, BUT
    // the window extends from x=1900 to x=3266 (well past the primary's right edge 1920). Only
    // 20px wide × 768 tall is on-screen — about 1.5% of the window area. The pre-fix validator
    // accepted this (top-left corner contained); the post-fix validator rejects via the 50%
    // intersection ratio rule.
    UserPreferences prefs = withWindow(1366, 768, 1900, 50, false);
    assertThat(
            StagePersistenceValidator.isStateValid(prefs, SINGLE_FHD_SCREEN, MIN_WIDTH, MIN_HEIGHT))
        .isFalse();
  }

  @Test
  void validWhenTopLeftIsOffscreenButMajorityOfWindowIsOnPrimary() {
    // F4.5 REVIEW F-002: window straddles the primary's left edge. Top-left at (-200, 100) is
    // outside the screen, but the window 1366x768 is mostly on-screen — intersection 1166x768
    // out of 1366x768 = ~85.4%. Above the 50% threshold so the state is restorable: the user
    // can still grab the title bar and drag it back.
    UserPreferences prefs = withWindow(1366, 768, -200, 100, false);
    assertThat(
            StagePersistenceValidator.isStateValid(prefs, SINGLE_FHD_SCREEN, MIN_WIDTH, MIN_HEIGHT))
        .isTrue();
  }

  @Test
  void invalidWhenWindowSpansMostlyTheUnpluggedSecondary() {
    // Window persisted spanning both monitors, mostly on the secondary. Disconnecting the
    // secondary leaves only ~20% of the window on the primary — below the 50% threshold.
    // Persisted: x=1700..3066 (1366 wide), screens (single primary 1920x1080) →
    // intersection = (1920-1700)*768 = 168960 vs total 1366*768 = 1049088 → ~16%.
    UserPreferences prefs = withWindow(1366, 768, 1700, 50, false);
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
