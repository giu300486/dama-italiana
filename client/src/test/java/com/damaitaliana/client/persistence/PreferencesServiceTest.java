package com.damaitaliana.client.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.client.app.ClientProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PreferencesServiceTest {

  @TempDir Path tempDir;
  private Path configFile;
  private PreferencesService service;

  @BeforeEach
  void setUp() {
    configFile = tempDir.resolve("config.json");
    var props = new ClientProperties(tempDir.resolve("saves").toString(), configFile.toString());
    service = new PreferencesService(props);
  }

  @Test
  void loadReturnsDefaultsWhenFileMissing() {
    UserPreferences prefs = service.load();
    assertThat(prefs).isEqualTo(UserPreferences.defaults());
    assertThat(prefs.locale()).isEqualTo(Locale.ITALIAN);
    assertThat(prefs.themeId()).isEqualTo("light");
    assertThat(prefs.uiScalePercent()).isEqualTo(100);
    assertThat(prefs.firstLaunch()).isTrue();
  }

  @Test
  void saveThenLoadRoundTrips() {
    var prefs =
        new UserPreferences(
            UserPreferences.CURRENT_SCHEMA_VERSION, Locale.ENGLISH, "light", 125, false);
    service.save(prefs);

    UserPreferences loaded = service.load();
    assertThat(loaded.schemaVersion()).isEqualTo(UserPreferences.CURRENT_SCHEMA_VERSION);
    assertThat(loaded.locale()).isEqualTo(Locale.ENGLISH);
    assertThat(loaded.themeId()).isEqualTo("light");
    assertThat(loaded.uiScalePercent()).isEqualTo(125);
    assertThat(loaded.firstLaunch()).isFalse();
  }

  @Test
  void saveCreatesParentDirectory() {
    Path nested = tempDir.resolve("a/b/c/config.json");
    var nestedService =
        new PreferencesService(
            new ClientProperties(tempDir.resolve("saves").toString(), nested.toString()));

    nestedService.save(UserPreferences.defaults());

    assertThat(Files.exists(nested)).isTrue();
  }

  @Test
  void loadFallsBackToDefaultsOnMalformedJson() throws IOException {
    Files.writeString(configFile, "not-valid-json{{{", StandardCharsets.UTF_8);
    UserPreferences loaded = service.load();
    assertThat(loaded).isEqualTo(UserPreferences.defaults());
  }

  @Test
  void loadFallsBackToDefaultsOnUnknownSchemaVersion() throws IOException {
    String forwardCompat =
        """
        {
          "schemaVersion": 99,
          "locale": "it",
          "themeId": "fancy-future-theme",
          "uiScalePercent": 200,
          "firstLaunch": false
        }
        """;
    Files.writeString(configFile, forwardCompat, StandardCharsets.UTF_8);

    UserPreferences loaded = service.load();
    assertThat(loaded).isEqualTo(UserPreferences.defaults());
  }

  @Test
  void saveDoesNotLeaveTempFileBehind() {
    service.save(UserPreferences.defaults());
    Path tmp = configFile.resolveSibling(configFile.getFileName() + ".tmp");
    assertThat(Files.exists(tmp)).as("tmp file %s", tmp).isFalse();
  }

  @Test
  void overwritingExistingConfigReplacesContent() {
    service.save(UserPreferences.defaults().withLocale(Locale.ITALIAN));
    service.save(UserPreferences.defaults().withLocale(Locale.ENGLISH));

    assertThat(service.load().locale()).isEqualTo(Locale.ENGLISH);
  }

  @Test
  void localeRoundTripsAsLanguageTag() throws IOException {
    service.save(UserPreferences.defaults().withLocale(Locale.ENGLISH));
    String json = Files.readString(configFile, StandardCharsets.UTF_8);
    assertThat(json).contains("\"locale\" : \"en\"");
  }

  @Test
  void deserializationDefaultsNullLocaleToItalian() throws IOException {
    String missingLocale =
        """
        {
          "schemaVersion": 1,
          "locale": null,
          "themeId": "light",
          "uiScalePercent": 100,
          "firstLaunch": true
        }
        """;
    Files.writeString(configFile, missingLocale, StandardCharsets.UTF_8);

    assertThat(service.load().locale()).isEqualTo(Locale.ITALIAN);
  }
}
