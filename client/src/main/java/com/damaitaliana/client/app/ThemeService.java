package com.damaitaliana.client.app;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Loads the design-system stylesheets and (optionally) the Inter font, then attaches them to a
 * {@link Scene}. Single source of truth for which CSS files compose a theme — callers should not
 * touch {@link Scene#getStylesheets()} directly.
 *
 * <p>Fase 3 always serves the light theme; the dark stylesheet ({@code /css/theme-dark.css}) is
 * tracked alongside but not selected (PLAN-fase-3 §7.3). The runtime toggle and WCAG AA
 * verification in dark mode are scheduled for Fase 11.
 *
 * <p>Font loading is best-effort: Inter (UI) and Playfair Display (display) variable TTFs live
 * under {@code /fonts/} as bundled resources (Fase 3.5, both SIL OFL 1.1). If a binary is missing
 * the service logs an INFO line and JavaFX falls back to the rest of the font-family chain declared
 * in the active stylesheet. See {@code client/src/main/resources/fonts/README.md}.
 */
@Component
public class ThemeService {

  private static final Logger log = LoggerFactory.getLogger(ThemeService.class);

  static final String THEME_LIGHT_PATH = "/css/theme-light.css";
  static final String THEME_DARK_PATH = "/css/theme-dark.css";
  static final String COMPONENTS_PATH = "/css/components.css";

  static final String INTER_VARIABLE_PATH = "/fonts/InterVariable.ttf";
  static final String PLAYFAIR_DISPLAY_VARIABLE_PATH = "/fonts/PlayfairDisplay-Variable.ttf";

  private volatile boolean fontsAttempted;

  /**
   * Replaces {@code scene}'s stylesheets with the active theme + components, in that order, and
   * triggers a one-time best-effort load of the Inter font on first call.
   */
  public void applyTheme(Scene scene) {
    Objects.requireNonNull(scene, "scene");
    scene.getStylesheets().setAll(stylesheetUrls());
    ensureFontsLoaded();
  }

  /**
   * URLs of the stylesheets composing the active theme, ordered theme-first then components so
   * component rules can reference theme colors via JavaFX looked-up colors.
   *
   * @throws IllegalStateException if any required stylesheet is missing from the classpath.
   */
  public List<String> stylesheetUrls() {
    return List.of(requireResource(THEME_LIGHT_PATH), requireResource(COMPONENTS_PATH));
  }

  private static String requireResource(String path) {
    URL url = ThemeService.class.getResource(path);
    if (url == null) {
      throw new IllegalStateException("Required stylesheet missing on classpath: " + path);
    }
    return url.toExternalForm();
  }

  private synchronized void ensureFontsLoaded() {
    if (fontsAttempted) {
      return;
    }
    fontsAttempted = true;
    tryLoadFont(INTER_VARIABLE_PATH);
    tryLoadFont(PLAYFAIR_DISPLAY_VARIABLE_PATH);
  }

  private void tryLoadFont(String resourcePath) {
    URL url = getClass().getResource(resourcePath);
    if (url == null) {
      log.info(
          "Font binary not bundled at {} — falling back to system font chain. See"
              + " resources/fonts/README.md to enable Inter.",
          resourcePath);
      return;
    }
    Font font = Font.loadFont(url.toExternalForm(), 14);
    if (font == null) {
      log.warn("Font.loadFont returned null for {}", resourcePath);
    } else {
      log.info("Loaded font: {} ({})", font.getName(), font.getFamily());
    }
  }
}
