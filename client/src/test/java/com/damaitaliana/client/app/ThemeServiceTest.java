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
  void themeLightDefinesAllSpecColorTokens() throws IOException {
    String css = readResource(ThemeService.THEME_LIGHT_PATH);
    assertThat(css)
        .contains("-color-bg-primary: #FAFAFA;")
        .contains("-color-bg-surface: #FFFFFF;")
        .contains("-color-text-primary: #18181B;")
        .contains("-color-text-secondary: #71717A;")
        .contains("-color-accent: #2563EB;")
        .contains("-color-accent-hover: #1D4ED8;")
        .contains("-color-success: #16A34A;")
        .contains("-color-warning: #EAB308;")
        .contains("-color-danger: #DC2626;")
        .contains("-color-board-light: #F0D9B5;")
        .contains("-color-board-dark: #B58863;")
        .contains("-color-piece-white: #FAFAFA;")
        .contains("-color-piece-black: #1F2937;")
        .contains("-color-highlight-legal: #FBBF24;")
        .contains("-color-highlight-mandatory: #DC2626;");
  }

  @Test
  void themeLightDefinesFontFamilyChain() throws IOException {
    String css = readResource(ThemeService.THEME_LIGHT_PATH);
    assertThat(css).contains("\"Inter\", \"Segoe UI\", \"Helvetica Neue\", sans-serif");
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
  void themeLightDefinesElevatedCardShadow() throws IOException {
    String css = readResource(ThemeService.THEME_LIGHT_PATH);
    assertThat(css).contains(".card-elevated").contains("dropshadow(gaussian");
  }

  @Test
  void themeDarkStubDefinesDarkPalette() throws IOException {
    String css = readResource(ThemeService.THEME_DARK_PATH);
    assertThat(css)
        .contains("-color-bg-primary: #0F172A;")
        .contains("-color-text-primary: #F1F5F9;")
        .contains("-color-accent: #3B82F6;");
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
  void interFontResourcesAreOptionalAndAbsenceIsAcceptable() {
    // Inter binaries are not committed (see resources/fonts/README.md). The service must not
    // fail when they are absent: callers rely on the font-family fallback chain.
    URL inter = getClass().getResource(ThemeService.INTER_REGULAR_PATH);
    assertThat(inter == null || inter.toExternalForm().endsWith(".ttf")).isTrue();
  }

  private String readResource(String path) throws IOException {
    try (var in = getClass().getResourceAsStream(path)) {
      assertThat(in).as("classpath resource %s", path).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
