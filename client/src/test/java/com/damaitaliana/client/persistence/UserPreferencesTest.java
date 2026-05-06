package com.damaitaliana.client.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * F4.5 REVIEW F-001 + F-011 — focused tests for the geometry-related corners of {@link
 * UserPreferences}: the {@code withWindowMaximized} flag-only helper introduced for the persist
 * decision flow, and the canonical-constructor defensive clamp that turns negative width/height
 * into {@code null} ("no persisted state") so a corrupted v3 file cannot poison subsequent loads.
 */
class UserPreferencesTest {

  @Test
  void withWindowMaximizedFlipsFlagAndPreservesAllOtherFields() {
    UserPreferences before =
        new UserPreferences(
            UserPreferences.CURRENT_SCHEMA_VERSION,
            Locale.ENGLISH,
            "light",
            125,
            false,
            45,
            85,
            true,
            false,
            1600,
            900,
            120,
            80,
            false);

    UserPreferences after = before.withWindowMaximized(true);

    assertThat(after.windowMaximized()).isTrue();
    assertThat(after.windowWidth()).isEqualTo(1600);
    assertThat(after.windowHeight()).isEqualTo(900);
    assertThat(after.windowX()).isEqualTo(120);
    assertThat(after.windowY()).isEqualTo(80);
    assertThat(after.locale()).isEqualTo(Locale.ENGLISH);
    assertThat(after.uiScalePercent()).isEqualTo(125);
    assertThat(after.musicVolumePercent()).isEqualTo(45);
    assertThat(after.sfxVolumePercent()).isEqualTo(85);
    assertThat(after.musicMuted()).isTrue();
    assertThat(after.sfxMuted()).isFalse();
  }

  @Test
  void withWindowMaximizedPreservesNullGeometryWhenAlreadyNull() {
    UserPreferences fresh = UserPreferences.defaults();

    UserPreferences afterMaximize = fresh.withWindowMaximized(true);

    assertThat(afterMaximize.windowMaximized()).isTrue();
    assertThat(afterMaximize.windowWidth()).isNull();
    assertThat(afterMaximize.windowHeight()).isNull();
    assertThat(afterMaximize.windowX()).isNull();
    assertThat(afterMaximize.windowY()).isNull();
  }

  @Test
  void canonicalConstructorClampsNegativeWidthAndHeightToNull() {
    UserPreferences poisoned =
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
            -1366,
            -768,
            100,
            50,
            false);

    assertThat(poisoned.windowWidth()).isNull();
    assertThat(poisoned.windowHeight()).isNull();
  }

  @Test
  void canonicalConstructorAllowsNegativeXAndYForMultiMonitorLayouts() {
    // Secondary monitor positioned to the left of the primary → x < 0 is legitimate.
    UserPreferences negativeOrigin =
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
            1920,
            1080,
            -1920,
            -100,
            false);

    assertThat(negativeOrigin.windowX()).isEqualTo(-1920);
    assertThat(negativeOrigin.windowY()).isEqualTo(-100);
    assertThat(negativeOrigin.windowWidth()).isEqualTo(1920);
    assertThat(negativeOrigin.windowHeight()).isEqualTo(1080);
  }
}
