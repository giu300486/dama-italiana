package com.damaitaliana.client.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

/**
 * End-to-end coverage of A3.10 localization parity: every i18n key consumed by the main controllers
 * (menu, setup, board chrome, save dialog, load screen, settings, rules) MUST resolve in both
 * Italian and English without falling through to the {@code [missing.key]} bracket form.
 *
 * <p>The list below intentionally hard-codes the keys controllers depend on rather than scraping
 * them at test-time so a future rename gives a clear, line-numbered failure here. The {@code
 * MessageSourceConfigTest#bothBundlesHaveSameKeySet} guard already enforces bundle symmetry; this
 * test pins the controller→bundle contract from the consumer side.
 */
class LocalizationE2ETest {

  private static final List<String> CONTROLLER_KEYS =
      List.of(
          // splash + app chrome
          "app.title",
          "splash.loading",
          // common buttons
          "common.button.back",
          "common.button.cancel",
          "common.button.save",
          // main menu
          "menu.open",
          "menu.singleplayer.title",
          "menu.singleplayer.subtitle",
          "menu.lan.title",
          "menu.lan.subtitle",
          "menu.lan.disabled",
          "menu.online.title",
          "menu.online.subtitle",
          "menu.online.disabled",
          "menu.rules.title",
          "menu.rules.subtitle",
          "menu.settings.title",
          "menu.settings.subtitle",
          // single-player setup
          "setup.title",
          "setup.level.label",
          "setup.level.principiante",
          "setup.level.esperto",
          "setup.level.campione",
          "setup.color.label",
          "setup.color.white",
          "setup.color.black",
          "setup.color.random",
          "setup.name.label",
          "setup.button.confirm",
          "setup.error.name.empty",
          // status pane
          "status.player.human",
          "status.player.ai",
          "status.player.color",
          "status.turn.white",
          "status.turn.black",
          "status.endgame.white_wins",
          "status.endgame.black_wins",
          "status.endgame.draw_repetition",
          "status.endgame.draw_forty_moves",
          "status.endgame.draw_agreement",
          "status.thinking",
          // board menu
          "board.menu.game",
          "board.menu.save",
          "board.menu.load",
          "board.menu.terminate",
          "board.menu.rules",
          // save dialog
          "save.dialog.title",
          "save.dialog.name.label",
          "save.dialog.name.empty",
          "save.dialog.name.invalid",
          // load screen
          "load.title",
          "load.column.name",
          "load.column.date",
          "load.column.level",
          "load.column.color",
          "load.column.move",
          "load.column.miniature",
          "load.button.load",
          "load.button.delete",
          "load.empty",
          // settings
          "settings.title",
          "settings.section.language",
          "settings.section.scaling",
          "settings.section.theme",
          "settings.scaling.100",
          "settings.scaling.125",
          "settings.scaling.150",
          "settings.theme.light",
          "settings.theme.disabled.note",
          "settings.button.save",
          // rules screen
          "rules.title",
          "rules.section.setup.title",
          "rules.section.setup.body",
          "rules.section.movement.title",
          "rules.section.movement.body",
          "rules.section.capture.title",
          "rules.section.capture.body",
          "rules.section.precedence.title",
          "rules.section.precedence.body",
          "rules.section.promotion.title",
          "rules.section.promotion.body",
          "rules.section.endgame.title",
          "rules.section.endgame.body",
          "rules.section.notation.title",
          "rules.section.notation.body",
          "rules.animation.play",
          "rules.animation.simple_capture.caption",
          "rules.animation.multi_capture.caption",
          "rules.animation.promotion.caption");

  private final MessageSource source = MessageSourceConfig.buildMessageSource();

  @Test
  void everyControllerKeyResolvesInItalian() {
    assertResolvesAll(Locale.ITALIAN);
  }

  @Test
  void everyControllerKeyResolvesInEnglish() {
    assertResolvesAll(Locale.ENGLISH);
  }

  @Test
  void italianAndEnglishValuesDifferForVisibleStrings() {
    // Spot-check that the bundles are not accidentally identical (e.g. someone copied the IT file
    // over the EN file). We pick keys whose IT/EN translations are clearly distinct.
    assertThat(source.getMessage("app.title", null, Locale.ITALIAN)).isEqualTo("Dama Italiana");
    assertThat(source.getMessage("app.title", null, Locale.ENGLISH)).isEqualTo("Italian Draughts");
    assertThat(source.getMessage("menu.rules.title", null, Locale.ITALIAN)).isEqualTo("Regole");
    assertThat(source.getMessage("menu.rules.title", null, Locale.ENGLISH)).isEqualTo("Rules");
  }

  @Test
  void parametrizedKeysFormatPlaceholders() {
    // status.player.color uses {0} and {1}; setup.name.default uses {0}. Verify MessageFormat
    // substitution works in both locales.
    String coloredIt =
        source.getMessage("status.player.color", new Object[] {"Tu", "Bianco"}, Locale.ITALIAN);
    assertThat(coloredIt).contains("Tu").contains("Bianco");

    String coloredEn =
        source.getMessage("status.player.color", new Object[] {"You", "White"}, Locale.ENGLISH);
    assertThat(coloredEn).contains("You").contains("White");

    String nameDefaultIt =
        source.getMessage("setup.name.default", new Object[] {"2026-04-29"}, Locale.ITALIAN);
    assertThat(nameDefaultIt).contains("2026-04-29");
  }

  private void assertResolvesAll(Locale locale) {
    for (String key : CONTROLLER_KEYS) {
      String value = source.getMessage(key, null, locale);
      assertThat(value)
          .as("key '%s' must resolve in locale %s", key, locale)
          .isNotNull()
          .isNotBlank()
          .doesNotStartWith("[");
    }
  }
}
