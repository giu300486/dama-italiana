package com.damaitaliana.client.i18n;

import com.damaitaliana.client.persistence.PreferencesService;
import com.damaitaliana.client.persistence.UserPreferences;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Holds the active UI locale.
 *
 * <p>The initial value is read from {@link PreferencesService#load()} at construction time so the
 * choice survives across launches. {@link #switchTo(Locale)} both updates the in-memory value and
 * writes the new locale back through {@link PreferencesService#save}, so the change is durable
 * without callers having to coordinate two beans.
 *
 * <p>Fase 3 keeps the UI binding restart-scoped (PLAN-fase-3 §7.10): re-rendering open scenes with
 * the new locale is a Fase 11 concern.
 */
@Component
public class LocaleService {

  private final PreferencesService preferencesService;
  private volatile Locale current;

  public LocaleService(PreferencesService preferencesService) {
    this.preferencesService = Objects.requireNonNull(preferencesService, "preferencesService");
    this.current = preferencesService.load().locale();
  }

  public Locale current() {
    return current;
  }

  public void switchTo(Locale newLocale) {
    Objects.requireNonNull(newLocale, "newLocale");
    if (newLocale.equals(this.current)) {
      return;
    }
    this.current = newLocale;
    UserPreferences prefs = preferencesService.load();
    preferencesService.save(prefs.withLocale(newLocale));
  }
}
