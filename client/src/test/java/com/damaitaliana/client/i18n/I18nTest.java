package com.damaitaliana.client.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.client.app.ClientProperties;
import com.damaitaliana.client.persistence.PreferencesService;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;

class I18nTest {

  @TempDir Path tempDir;

  private LocaleService localeService;
  private StaticMessageSource staticSource;
  private I18n i18n;

  @BeforeEach
  void setUp() {
    var props =
        new ClientProperties(
            tempDir.resolve("saves").toString(), tempDir.resolve("config.json").toString());
    localeService = new LocaleService(new PreferencesService(props));
    staticSource = new StaticMessageSource();
    i18n = new I18n(staticSource, localeService);
  }

  @Test
  void resolvesViaCurrentLocale() {
    staticSource.addMessage("greeting", Locale.ITALIAN, "Ciao");
    staticSource.addMessage("greeting", Locale.ENGLISH, "Hello");

    assertThat(i18n.t("greeting")).isEqualTo("Ciao");
    localeService.switchTo(Locale.ENGLISH);
    assertThat(i18n.t("greeting")).isEqualTo("Hello");
  }

  @Test
  void formatsArgumentsViaMessageFormat() {
    staticSource.addMessage("welcome", Locale.ITALIAN, "Benvenuto, {0}!");
    assertThat(i18n.t("welcome", "Marco")).isEqualTo("Benvenuto, Marco!");
  }

  @Test
  void missingKeyReturnsBracketedPlaceholder() {
    assertThat(i18n.t("does.not.exist")).isEqualTo("[does.not.exist]");
  }

  @Test
  void resolvesAgainstActualBundles() {
    MessageSource real = MessageSourceConfig.buildMessageSource();
    I18n realI18n = new I18n(real, localeService);

    assertThat(realI18n.t("app.title")).isEqualTo("Dama Italiana");
    localeService.switchTo(Locale.ENGLISH);
    assertThat(realI18n.t("app.title")).isEqualTo("Italian Draughts");
  }
}
