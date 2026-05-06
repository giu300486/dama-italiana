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
 * <p>v3 (Fase 4.5, PLAN-fase-4.5 Task 4.5.7b): adds Stage state persistence — {@code windowWidth},
 * {@code windowHeight}, {@code windowX}, {@code windowY}, {@code windowMaximized}. The four
 * geometry fields are nullable to model "no persisted state yet — use the computed 80% fallback"
 * (e.g. fresh install or v2 → v3 migration). v2 files are migrated transparently with the geometry
 * fields left null. The validator {@link com.damaitaliana.client.app.StagePersistenceValidator}
 * decides whether the persisted state is safe to restore on the current display set.
 *
 * @param schemaVersion always {@link #CURRENT_SCHEMA_VERSION} for files written by Fase 4.5.
 * @param locale active UI language (default Italian, SPEC §13.6).
 * @param themeId active theme identifier; only {@code "light"} is selectable in Fase 3 (PLAN-fase-3
 *     §7.3 — dark toggle in Fase 11).
 * @param uiScalePercent UI scaling factor; one of 100/125/150 (SPEC §13.5).
 * @param firstLaunch true until the user dismisses the welcome flow at least once.
 * @param musicVolumePercent ambient music bus volume 0-100 (default 30, SPEC §13.4).
 * @param sfxVolumePercent gameplay SFX bus volume 0-100 (default 70, SPEC §13.4).
 * @param musicMuted user-toggled mute for the music bus (independent of volume).
 * @param sfxMuted user-toggled mute for the SFX bus.
 * @param windowWidth last known Stage width in logical pixels, or {@code null} when no state has
 *     been persisted yet.
 * @param windowHeight last known Stage height in logical pixels, or {@code null}.
 * @param windowX last known Stage top-left X in logical pixels (multi-monitor screen coordinates),
 *     or {@code null}.
 * @param windowY last known Stage top-left Y, or {@code null}.
 * @param windowMaximized whether the Stage was maximised when last closed.
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
    boolean sfxMuted,
    Integer windowWidth,
    Integer windowHeight,
    Integer windowX,
    Integer windowY,
    boolean windowMaximized) {

  public static final int CURRENT_SCHEMA_VERSION = 3;
  public static final int DEFAULT_MUSIC_VOLUME_PERCENT = 30;
  public static final int DEFAULT_SFX_VOLUME_PERCENT = 70;

  public UserPreferences {
    locale = locale != null ? locale : Locale.ITALIAN;
    themeId = themeId != null ? themeId : "light";
    uiScalePercent = uiScalePercent > 0 ? uiScalePercent : 100;
    musicVolumePercent = clampVolume(musicVolumePercent);
    sfxVolumePercent = clampVolume(sfxVolumePercent);
    // F4.5 REVIEW F-011: reject negative width/height — null means "no persisted state".
    // x/y may legitimately be negative on multi-monitor setups (secondary left of primary),
    // so they are not clamped.
    if (windowWidth != null && windowWidth < 0) {
      windowWidth = null;
    }
    if (windowHeight != null && windowHeight < 0) {
      windowHeight = null;
    }
  }

  /**
   * Jackson factory used for deserialisation. Boxed types for the audio and window fields so a
   * missing key (older schema file) is detected as {@code null} and defaulted appropriately.
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
      @JsonProperty("sfxMuted") Boolean sfxMuted,
      @JsonProperty("windowWidth") Integer windowWidth,
      @JsonProperty("windowHeight") Integer windowHeight,
      @JsonProperty("windowX") Integer windowX,
      @JsonProperty("windowY") Integer windowY,
      @JsonProperty("windowMaximized") Boolean windowMaximized) {
    return new UserPreferences(
        schemaVersion,
        locale,
        themeId,
        uiScalePercent,
        firstLaunch,
        musicVolumePercent != null ? musicVolumePercent : DEFAULT_MUSIC_VOLUME_PERCENT,
        sfxVolumePercent != null ? sfxVolumePercent : DEFAULT_SFX_VOLUME_PERCENT,
        musicMuted != null && musicMuted,
        sfxMuted != null && sfxMuted,
        windowWidth,
        windowHeight,
        windowX,
        windowY,
        windowMaximized != null && windowMaximized);
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
        false,
        null,
        null,
        null,
        null,
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
        sfxMuted,
        windowWidth,
        windowHeight,
        windowX,
        windowY,
        windowMaximized);
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
        sfxMuted,
        windowWidth,
        windowHeight,
        windowX,
        windowY,
        windowMaximized);
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
        sfxMuted,
        windowWidth,
        windowHeight,
        windowX,
        windowY,
        windowMaximized);
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
        sfxMuted,
        windowWidth,
        windowHeight,
        windowX,
        windowY,
        windowMaximized);
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
        sfxMuted,
        windowWidth,
        windowHeight,
        windowX,
        windowY,
        windowMaximized);
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
        sfxMuted,
        windowWidth,
        windowHeight,
        windowX,
        windowY,
        windowMaximized);
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
        sfxMuted,
        windowWidth,
        windowHeight,
        windowX,
        windowY,
        windowMaximized);
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
        newSfxMuted,
        windowWidth,
        windowHeight,
        windowX,
        windowY,
        windowMaximized);
  }

  /**
   * F4.5 Task 4.5.7b — sets the Stage state fields all together (geometry is captured atomically on
   * close).
   */
  public UserPreferences withWindowState(int width, int height, int x, int y, boolean maximized) {
    return new UserPreferences(
        schemaVersion,
        locale,
        themeId,
        uiScalePercent,
        firstLaunch,
        musicVolumePercent,
        sfxVolumePercent,
        musicMuted,
        sfxMuted,
        width,
        height,
        x,
        y,
        maximized);
  }

  /**
   * F4.5 REVIEW F-001 — flips only {@code windowMaximized} while preserving the previously
   * persisted geometry. Used by {@link com.damaitaliana.client.app.StagePersistenceCoordinator}
   * when the stage closes while maximized but no windowed-bounds memory was tracked, so the
   * persisted windowed geometry from a previous session must be carried forward.
   */
  public UserPreferences withWindowMaximized(boolean newWindowMaximized) {
    return new UserPreferences(
        schemaVersion,
        locale,
        themeId,
        uiScalePercent,
        firstLaunch,
        musicVolumePercent,
        sfxVolumePercent,
        musicMuted,
        sfxMuted,
        windowWidth,
        windowHeight,
        windowX,
        windowY,
        newWindowMaximized);
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
        sfxMuted,
        windowWidth,
        windowHeight,
        windowX,
        windowY,
        windowMaximized);
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
