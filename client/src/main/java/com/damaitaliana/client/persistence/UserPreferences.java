package com.damaitaliana.client.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;

/**
 * Persistent client-side user preferences. Schema versioned ({@link #CURRENT_SCHEMA_VERSION}) so
 * future breaking changes can be detected and migrated explicitly rather than silently
 * mis-deserialised.
 *
 * <p>Fase 3 fields cover only what the UI needs in single-player mode. Fields like the
 * AES-GCM-encrypted JWT (SPEC §14.1) are deliberately omitted until Fase 5/6 introduces
 * authentication — adding the field then bumps {@link #CURRENT_SCHEMA_VERSION} to 2.
 *
 * @param schemaVersion always {@link #CURRENT_SCHEMA_VERSION} for files written by Fase 3.
 * @param locale active UI language (default Italian, SPEC §13.6).
 * @param themeId active theme identifier; only {@code "light"} is selectable in Fase 3 (PLAN-fase-3
 *     §7.3 — dark toggle in Fase 11).
 * @param uiScalePercent UI scaling factor; one of 100/125/150 (SPEC §13.5).
 * @param firstLaunch true until the user dismisses the welcome flow at least once.
 */
public record UserPreferences(
    int schemaVersion, Locale locale, String themeId, int uiScalePercent, boolean firstLaunch) {

  public static final int CURRENT_SCHEMA_VERSION = 1;

  @JsonCreator
  public UserPreferences(
      @JsonProperty("schemaVersion") int schemaVersion,
      @JsonProperty("locale") Locale locale,
      @JsonProperty("themeId") String themeId,
      @JsonProperty("uiScalePercent") int uiScalePercent,
      @JsonProperty("firstLaunch") boolean firstLaunch) {
    this.schemaVersion = schemaVersion;
    this.locale = locale != null ? locale : Locale.ITALIAN;
    this.themeId = themeId != null ? themeId : "light";
    this.uiScalePercent = uiScalePercent > 0 ? uiScalePercent : 100;
    this.firstLaunch = firstLaunch;
  }

  public static UserPreferences defaults() {
    return new UserPreferences(CURRENT_SCHEMA_VERSION, Locale.ITALIAN, "light", 100, true);
  }

  public UserPreferences withLocale(Locale newLocale) {
    return new UserPreferences(schemaVersion, newLocale, themeId, uiScalePercent, firstLaunch);
  }

  public UserPreferences withFirstLaunch(boolean firstLaunch) {
    return new UserPreferences(schemaVersion, locale, themeId, uiScalePercent, firstLaunch);
  }
}
