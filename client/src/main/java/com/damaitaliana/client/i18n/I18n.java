package com.damaitaliana.client.i18n;

import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

/**
 * Convenience helper that resolves UI strings against the active locale. FXML controllers inject
 * this single bean instead of carrying both {@link MessageSource} and {@link LocaleService}.
 *
 * <p>Unresolved keys are returned wrapped in square brackets ({@code [missing.key]}) rather than
 * throwing — visually obvious in the UI during development and harmless at runtime. The {@code
 * MessageSourceConfigTest#bothBundlesHaveSameKeySet} guard keeps drift between the IT and EN
 * bundles from going unnoticed.
 */
@Component
public class I18n {

  private final MessageSource messageSource;
  private final LocaleService localeService;

  public I18n(MessageSource messageSource, LocaleService localeService) {
    this.messageSource = Objects.requireNonNull(messageSource, "messageSource");
    this.localeService = Objects.requireNonNull(localeService, "localeService");
  }

  /** Resolves {@code code} against the active locale; returns {@code [code]} when missing. */
  public String t(String code) {
    return t(code, (Object[]) null);
  }

  /** {@link java.text.MessageFormat}-style argument substitution. */
  public String t(String code, Object... args) {
    Objects.requireNonNull(code, "code");
    try {
      return messageSource.getMessage(code, args, localeService.current());
    } catch (NoSuchMessageException ex) {
      return "[" + code + "]";
    }
  }
}
