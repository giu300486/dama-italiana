package com.damaitaliana.client.app;

import com.damaitaliana.client.persistence.PreferencesService;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Applies the user's UI scaling preference (SPEC §13.5 — 100/125/150%) to a {@link Scene} by
 * setting {@code -fx-font-size} on its root. Re-evaluating the preference per scene switch is the
 * mechanism that makes scaling feel global from a single source of truth ({@link
 * PreferencesService}).
 *
 * <p>Caller contract:
 *
 * <ul>
 *   <li>{@link #applyTo(Scene)} reads the current preference and applies — wired into {@link
 *       SceneRouter#show(SceneId)} so every navigation honors the user's choice.
 *   <li>{@link #applyTo(Scene, int)} applies a specific percent without touching disk — used by
 *       {@link com.damaitaliana.client.ui.settings.SettingsController SettingsController} to give
 *       the user a live preview while they pick a scaling step before saving.
 * </ul>
 *
 * <p>Unrecognised scaling values fall back to 100% rather than throw, so a stale or hand-edited
 * config never bricks the app. Allowed steps are {@link #ALLOWED_SCALES}.
 */
@Component
public class UiScalingService {

  private static final Logger log = LoggerFactory.getLogger(UiScalingService.class);

  /** Whitelisted scaling steps (SPEC §13.5). */
  public static final List<Integer> ALLOWED_SCALES = List.of(100, 125, 150);

  /** Base font size in pixels at 100% scaling. */
  public static final double BASE_FONT_SIZE_PX = 14.0;

  private final PreferencesService preferencesService;

  public UiScalingService(PreferencesService preferencesService) {
    this.preferencesService = Objects.requireNonNull(preferencesService, "preferencesService");
  }

  /** Reads the current preference and applies it to {@code scene}. */
  public void applyTo(Scene scene) {
    int percent = preferencesService.load().uiScalePercent();
    applyTo(scene, percent);
  }

  /** Applies {@code percent} (one of {@link #ALLOWED_SCALES}; out-of-range falls back to 100). */
  public void applyTo(Scene scene, int percent) {
    if (scene == null || scene.getRoot() == null) {
      return;
    }
    int safe = ALLOWED_SCALES.contains(percent) ? percent : 100;
    if (safe != percent) {
      log.warn("Unrecognised UI scaling value {}, falling back to 100", percent);
    }
    double sizePx = BASE_FONT_SIZE_PX * safe / 100.0;
    scene.getRoot().setStyle(String.format(Locale.ROOT, "-fx-font-size: %.1fpx;", sizePx));
  }
}
