package com.damaitaliana.client.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;

/**
 * Persistent client-side user preferences. Schema versioned ({@link #CURRENT_SCHEMA_VERSION}) so
 * future breaking changes can be detected and migrated explicitly rather than silently
 * mis-deserialised.
 *
 * <p>v1 (Fase 3) shipped: locale, themeId, uiScalePercent, firstLaunch.
 *
 * <p>v2 (Fase 3.5, SPEC §13.4): adds audio persistence — {@code musicVolumePercent}, {@code
 * sfxVolumePercent}, {@code musicMuted}, {@code sfxMuted}. Defaults 30/70/false/false. v1 files are
 * migrated transparently by {@link PreferencesService#load()} (audio fields filled with defaults).
 *
 * @param schemaVersion always {@link #CURRENT_SCHEMA_VERSION} for files written by Fase 3.5.
 * @param locale active UI language (default Italian, SPEC §13.6).
 * @param themeId active theme identifier; only {@code "light"} is selectable in Fase 3 (PLAN-fase-3
 *     §7.3 — dark toggle in Fase 11).
 * @param uiScalePercent UI scaling factor; one of 100/125/150 (SPEC §13.5).
 * @param firstLaunch true until the user dismisses the welcome flow at least once.
 * @param musicVolumePercent ambient music bus volume 0-100 (default 30, SPEC §13.4).
 * @param sfxVolumePercent gameplay SFX bus volume 0-100 (default 70, SPEC §13.4).
 * @param musicMuted user-toggled mute for the music bus (independent of volume).
 * @param sfxMuted user-toggled mute for the SFX bus.
 */
public record UserPreferences(
    int schemaVersion,
    Locale locale,
    String themeId,
    int uiScalePercent,
    boolean firstLaunch,
    int musicVolumePercent,
    int sfxVolumePercent,
    boolean musicMuted,
    boolean sfxMuted) {

  public static final int CURRENT_SCHEMA_VERSION = 2;
  public static final int DEFAULT_MUSIC_VOLUME_PERCENT = 30;
  public static final int DEFAULT_SFX_VOLUME_PERCENT = 70;

  public UserPreferences {
    locale = locale != null ? locale : Locale.ITALIAN;
    themeId = themeId != null ? themeId : "light";
    uiScalePercent = uiScalePercent > 0 ? uiScalePercent : 100;
    musicVolumePercent = clampVolume(musicVolumePercent);
    sfxVolumePercent = clampVolume(sfxVolumePercent);
  }

  /**
   * Jackson factory used for deserialisation. Boxed {@code Integer}/{@code Boolean} for the audio
   * fields so a missing key (v1 file) is detected as {@code null} and defaulted to the SPEC §13.4
   * values rather than to the primitive {@code 0}/{@code false}.
   */
  @JsonCreator
  static UserPreferences fromJson(
      @JsonProperty("schemaVersion") int schemaVersion,
      @JsonProperty("locale") Locale locale,
      @JsonProperty("themeId") String themeId,
      @JsonProperty("uiScalePercent") int uiScalePercent,
      @JsonProperty("firstLaunch") boolean firstLaunch,
      @JsonProperty("musicVolumePercent") Integer musicVolumePercent,
      @JsonProperty("sfxVolumePercent") Integer sfxVolumePercent,
      @JsonProperty("musicMuted") Boolean musicMuted,
      @JsonProperty("sfxMuted") Boolean sfxMuted) {
    return new UserPreferences(
        schemaVersion,
        locale,
        themeId,
        uiScalePercent,
        firstLaunch,
        musicVolumePercent != null ? musicVolumePercent : DEFAULT_MUSIC_VOLUME_PERCENT,
        sfxVolumePercent != null ? sfxVolumePercent : DEFAULT_SFX_VOLUME_PERCENT,
        musicMuted != null && musicMuted,
        sfxMuted != null && sfxMuted);
  }

  public static UserPreferences defaults() {
    return new UserPreferences(
        CURRENT_SCHEMA_VERSION,
        Locale.ITALIAN,
        "light",
        100,
        true,
        DEFAULT_MUSIC_VOLUME_PERCENT,
        DEFAULT_SFX_VOLUME_PERCENT,
        false,
        false);
  }

  public UserPreferences withLocale(Locale newLocale) {
    return new UserPreferences(
        schemaVersion,
        newLocale,
        themeId,
        uiScalePercent,
        firstLaunch,
        musicVolumePercent,
        sfxVolumePercent,
        musicMuted,
        sfxMuted);
  }

  public UserPreferences withUiScalePercent(int newUiScalePercent) {
    return new UserPreferences(
        schemaVersion,
        locale,
        themeId,
        newUiScalePercent,
        firstLaunch,
        musicVolumePercent,
        sfxVolumePercent,
        musicMuted,
        sfxMuted);
  }

  public UserPreferences withThemeId(String newThemeId) {
    return new UserPreferences(
        schemaVersion,
        locale,
        newThemeId,
        uiScalePercent,
        firstLaunch,
        musicVolumePercent,
        sfxVolumePercent,
        musicMuted,
        sfxMuted);
  }

  public UserPreferences withFirstLaunch(boolean newFirstLaunch) {
    return new UserPreferences(
        schemaVersion,
        locale,
        themeId,
        uiScalePercent,
        newFirstLaunch,
        musicVolumePercent,
        sfxVolumePercent,
        musicMuted,
        sfxMuted);
  }

  public UserPreferences withMusicVolumePercent(int newMusicVolumePercent) {
    return new UserPreferences(
        schemaVersion,
        locale,
        themeId,
        uiScalePercent,
        firstLaunch,
        newMusicVolumePercent,
        sfxVolumePercent,
        musicMuted,
        sfxMuted);
  }

  public UserPreferences withSfxVolumePercent(int newSfxVolumePercent) {
    return new UserPreferences(
        schemaVersion,
        locale,
        themeId,
        uiScalePercent,
        firstLaunch,
        musicVolumePercent,
        newSfxVolumePercent,
        musicMuted,
        sfxMuted);
  }

  public UserPreferences withMusicMuted(boolean newMusicMuted) {
    return new UserPreferences(
        schemaVersion,
        locale,
        themeId,
        uiScalePercent,
        firstLaunch,
        musicVolumePercent,
        sfxVolumePercent,
        newMusicMuted,
        sfxMuted);
  }

  public UserPreferences withSfxMuted(boolean newSfxMuted) {
    return new UserPreferences(
        schemaVersion,
        locale,
        themeId,
        uiScalePercent,
        firstLaunch,
        musicVolumePercent,
        sfxVolumePercent,
        musicMuted,
        newSfxMuted);
  }

  /**
   * Returns a copy with {@code schemaVersion} replaced. Used by {@link PreferencesService} after
   * migrating an older config file in-memory.
   */
  UserPreferences withSchemaVersion(int newSchemaVersion) {
    return new UserPreferences(
        newSchemaVersion,
        locale,
        themeId,
        uiScalePercent,
        firstLaunch,
        musicVolumePercent,
        sfxVolumePercent,
        musicMuted,
        sfxMuted);
  }

  private static int clampVolume(int v) {
    if (v < 0) {
      return 0;
    }
    if (v > 100) {
      return 100;
    }
    return v;
  }
}
