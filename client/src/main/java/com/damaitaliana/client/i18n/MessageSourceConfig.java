package com.damaitaliana.client.i18n;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * Wires the {@link MessageSource} the rest of the client uses to look up UI strings.
 *
 * <p>Italian is the fallback locale (ADR-033, SPEC §13.6): if the active locale is neither {@code
 * it} nor {@code en} the bundle falls back to {@code messages_it.properties} rather than throwing.
 * UTF-8 is enforced explicitly because the platform default on Windows ({@code windows-1252}) would
 * mangle accented characters in the Italian bundle.
 *
 * <p>{@code useCodeAsDefaultMessage} is left disabled so unresolved keys raise {@link
 * org.springframework.context.NoSuchMessageException}; the {@link I18n} helper then converts that
 * to a clearly-bracketed placeholder ({@code [missing.key]}) so missing translations are visually
 * obvious during development.
 */
@Configuration
public class MessageSourceConfig {

  @Bean
  public MessageSource messageSource() {
    return buildMessageSource();
  }

  public static ReloadableResourceBundleMessageSource buildMessageSource() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:i18n/messages");
    source.setDefaultEncoding(StandardCharsets.UTF_8.name());
    source.setDefaultLocale(Locale.ITALIAN);
    source.setFallbackToSystemLocale(false);
    source.setUseCodeAsDefaultMessage(false);
    return source;
  }
}
