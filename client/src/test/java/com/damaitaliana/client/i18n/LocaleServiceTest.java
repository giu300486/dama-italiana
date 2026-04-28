package com.damaitaliana.client.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.damaitaliana.client.app.ClientProperties;
import com.damaitaliana.client.persistence.PreferencesService;
import com.damaitaliana.client.persistence.UserPreferences;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocaleServiceTest {

  @TempDir Path tempDir;

  private PreferencesService preferences;
  private LocaleService service;

  @BeforeEach
  void setUp() {
    var props =
        new ClientProperties(
            tempDir.resolve("saves").toString(), tempDir.resolve("config.json").toString());
    preferences = new PreferencesService(props);
    service = new LocaleService(preferences);
  }

  @Test
  void initialLocaleIsItalianWhenConfigMissing() {
    assertThat(service.current()).isEqualTo(Locale.ITALIAN);
  }

  @Test
  void initialLocaleIsReadFromPreferencesWhenPresent() {
    preferences.save(UserPreferences.defaults().withLocale(Locale.ENGLISH));
    var fresh = new LocaleService(preferences);
    assertThat(fresh.current()).isEqualTo(Locale.ENGLISH);
  }

  @Test
  void switchToEnglishUpdatesCurrent() {
    service.switchTo(Locale.ENGLISH);
    assertThat(service.current()).isEqualTo(Locale.ENGLISH);
  }

  @Test
  void switchToPersistsPreference() {
    service.switchTo(Locale.ENGLISH);
    assertThat(preferences.load().locale()).isEqualTo(Locale.ENGLISH);
  }

  @Test
  void switchToNullThrows() {
    assertThatNullPointerException().isThrownBy(() -> service.switchTo(null));
  }

  @Test
  void switchToSameLocaleIsNoOp() {
    service.switchTo(Locale.ITALIAN);
    // No file should have been written: load() still returns defaults (firstLaunch=true).
    assertThat(preferences.load().firstLaunch()).isTrue();
  }
}
