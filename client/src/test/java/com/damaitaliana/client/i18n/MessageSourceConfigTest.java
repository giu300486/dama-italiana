package com.damaitaliana.client.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

class MessageSourceConfigTest {

  private final MessageSource source = MessageSourceConfig.buildMessageSource();

  @Test
  void messageResolvesInItalian() {
    assertThat(source.getMessage("app.title", null, Locale.ITALIAN)).isEqualTo("Dama Italiana");
    assertThat(source.getMessage("common.button.cancel", null, Locale.ITALIAN))
        .isEqualTo("Annulla");
  }

  @Test
  void messageResolvesInEnglish() {
    assertThat(source.getMessage("app.title", null, Locale.ENGLISH)).isEqualTo("Italian Draughts");
    assertThat(source.getMessage("common.button.cancel", null, Locale.ENGLISH)).isEqualTo("Cancel");
  }

  @Test
  void unknownLocaleFallsBackToItalian() {
    // German is not provided. Expected fallback chain: messages_de → messages_it (defaultLocale).
    assertThat(source.getMessage("app.title", null, Locale.GERMAN)).isEqualTo("Dama Italiana");
  }

  @Test
  void utf8AccentedCharactersDecodeCorrectly() {
    // Italian "Caricamento…" includes a non-ASCII ellipsis (U+2026); without UTF-8 it would
    // come back mangled on Windows.
    assertThat(source.getMessage("splash.loading", null, Locale.ITALIAN)).contains("…");
  }

  @Test
  void missingKeyThrowsNoSuchMessageException() {
    // I18n converts this to "[missing.key]"; here we verify the underlying source's contract.
    assertThatThrownBy(() -> source.getMessage("definitely.missing.key", null, Locale.ITALIAN))
        .isInstanceOf(NoSuchMessageException.class);
  }

  @Test
  void bothBundlesHaveSameKeySet() throws IOException {
    Properties it = readBundle("/i18n/messages_it.properties");
    Properties en = readBundle("/i18n/messages_en.properties");
    assertThat(en.keySet())
        .as("keys in messages_en.properties match messages_it.properties")
        .containsExactlyInAnyOrderElementsOf(it.keySet());
  }

  private Properties readBundle(String classpath) throws IOException {
    Properties p = new Properties();
    try (var in = getClass().getResourceAsStream(classpath)) {
      assertThat(in).as("resource %s", classpath).isNotNull();
      p.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
    }
    return p;
  }
}
