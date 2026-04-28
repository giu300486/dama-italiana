package com.damaitaliana.client.i18n;

import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Holds the active UI locale.
 *
 * <p>Initialised to {@link Locale#ITALIAN} (SPEC §13.6 default). Task 3.4 will wire it to {@code
 * PreferencesService} so the choice survives across launches; Fase 3 keeps the change
 * restart-scoped (PLAN-fase-3 §7.10), so {@link #switchTo(Locale)} only updates the in-memory field
 * and persisting/reloading is handled outside this bean.
 */
@Component
public class LocaleService {

  private volatile Locale current = Locale.ITALIAN;

  public Locale current() {
    return current;
  }

  public void switchTo(Locale newLocale) {
    Objects.requireNonNull(newLocale, "newLocale");
    this.current = newLocale;
  }
}
