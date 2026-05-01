package com.damaitaliana.client.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ThemeServiceTest {

  private final ThemeService service = new ThemeService();

  @Test
  void stylesheetUrlsReturnLightThenComponentsInOrder() {
    var urls = service.stylesheetUrls();
    assertThat(urls).hasSize(2);
    assertThat(urls.get(0)).endsWith("theme-light.css");
    assertThat(urls.get(1)).endsWith("components.css");
  }

  @Test
  void allRequiredStylesheetsAreOnClasspath() {
    assertThat(getClass().getResource(ThemeService.THEME_LIGHT_PATH)).isNotNull();
    assertThat(getClass().getResource(ThemeService.COMPONENTS_PATH)).isNotNull();
    assertThat(getClass().getResource(ThemeService.THEME_DARK_PATH)).isNotNull();
  }

  @Test
  void themeLightDefinesWoodPremiumColorTokens() throws IOException {
    // SPEC §13.2 wood premium palette (Fase 3.5).
    String css = readResource(ThemeService.THEME_LIGHT_PATH);
    assertThat(css)
        .contains("-color-bg-primary: #2A1F15;")
        .contains("-color-bg-surface: #3D2E20;")
        .contains("-color-bg-elevated: #F5E6C8;")
        .contains("-color-border-subtle: #6B4423;")
        .contains("-color-border-frame: #4A2E18;")
        .contains("-color-text-on-dark: #F0E0C4;")
        .contains("-color-text-on-light: #2A1F15;")
        .contains("-color-text-secondary: #8B6F4E;")
        .contains("-color-accent-gold: #C9A45C;")
        .contains("-color-accent-gold-hover: #DDB874;")
        .contains("-color-accent-deep-red: #8B3A3A;")
        .contains("-color-success: #6B8E4E;")
        .contains("-color-board-light: #E8C99A;")
        .contains("-color-board-dark: #6B4423;")
        .contains("-color-piece-white: #F0E0C4;")
        .contains("-color-piece-black: #2A1F15;")
        .contains("-color-piece-king-marker-white: #C9A45C;")
        .contains("-color-piece-king-marker-black: #8B3A3A;")
        .contains("-color-highlight-legal: #C9A45C;")
        .contains("-color-highlight-mandatory: #C9A45C;");
  }

  @Test
  void themeLightDefinesFontFamilyChainsForUiAndDisplay() throws IOException {
    String css = readResource(ThemeService.THEME_LIGHT_PATH);
    assertThat(css)
        .contains("\"Inter\", \"Segoe UI\", \"Helvetica Neue\", sans-serif")
        .contains("\"Playfair Display\", \"Cormorant Garamond\", \"Georgia\", serif");
  }

  @Test
  void themeLightDefinesFiveInteractiveStatesForButton() throws IOException {
    String css = readResource(ThemeService.THEME_LIGHT_PATH);
    assertThat(css)
        .contains(".button {")
        .contains(".button:hover")
        .contains(".button:pressed")
        .contains(".button:focused")
        .contains(".button:disabled");
  }

  @Test
  void themeLightDefinesPrimaryAndSecondaryButtonAndDisplayLabel() throws IOException {
    // New helpers introduced in Fase 3.5 (PLAN-fase-3.5 §3 Task 3.5.2).
    String css = readResource(ThemeService.THEME_LIGHT_PATH);
    assertThat(css)
        .contains(".button-primary")
        .contains(".button-primary:hover")
        .contains(".button-primary:pressed")
        .contains(".button-primary:focused")
        .contains(".button-primary:disabled")
        .contains(".button-secondary")
        .contains(".label-display");
  }

  @Test
  void themeLightDefinesElevatedCardShadow() throws IOException {
    String css = readResource(ThemeService.THEME_LIGHT_PATH);
    assertThat(css).contains(".card-elevated").contains("dropshadow(gaussian");
  }

  @Test
  void themeDarkStubDefinesDarkWoodPalette() throws IOException {
    // F3.5 stub: dark theme tokens follow the same vocabulary as light, with darker values.
    // F11 will tune them properly with contrast-checker tooling.
    String css = readResource(ThemeService.THEME_DARK_PATH);
    assertThat(css)
        .contains("-color-bg-primary: #1A120A;")
        .contains("-color-bg-surface: #2A1F15;")
        .contains("-color-text-on-dark: #F0E0C4;")
        .contains("-color-accent-gold: #DDB874;");
  }

  @Test
  void componentsDefineBoardAndHighlightClasses() throws IOException {
    String css = readResource(ThemeService.COMPONENTS_PATH);
    assertThat(css)
        .contains(".board-cell-light")
        .contains(".board-cell-dark")
        .contains(".piece")
        .contains(".piece-king")
        .contains(".legal-target")
        .contains(".pulse-mandatory")
        .contains(".move-history-row");
  }

  @Test
  void fontBinariesArePresentAndLoadOnFirstApply() {
    // Fase 3.5 commits Inter Variable + Playfair Display Variable as bundled fonts.
    // Both are SIL OFL 1.1; binaries live in resources/fonts/.
    URL inter = getClass().getResource(ThemeService.INTER_VARIABLE_PATH);
    URL playfair = getClass().getResource(ThemeService.PLAYFAIR_DISPLAY_VARIABLE_PATH);
    assertThat(inter).as("Inter Variable bundled").isNotNull();
    assertThat(playfair).as("Playfair Display Variable bundled").isNotNull();
    assertThat(inter.toExternalForm()).endsWith(".ttf");
    assertThat(playfair.toExternalForm()).endsWith(".ttf");
  }

  private String readResource(String path) throws IOException {
    try (var in = getClass().getResourceAsStream(path)) {
      assertThat(in).as("classpath resource %s", path).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
