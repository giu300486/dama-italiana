package com.damaitaliana.client.audio;

/**
 * Catalog of one-shot sound effects fired from gameplay events. Each value names the bundled
 * resource under {@code /assets/audio/sfx/} (Task 3.5.1, all Kenney CC0 1.0 — see CREDITS.md).
 *
 * <p>The PLAN-fase-3.5 §3.5.3 originally listed four values (MOVE, CAPTURE, PROMOTION, VICTORY);
 * Task 3.5.1 acquired six because {@code illegal} and {@code lose} are also useful gameplay cues
 * and the assets are CC0. The deviation is documented in {@code AI_CONTEXT.md}.
 *
 * <p>The bundled files are 16-bit PCM WAV (Task 3.5.4 follow-up): JavaFX Media on Windows does not
 * decode OGG Vorbis, so the original Kenney {@code *.ogg} masters (kept under {@code
 * assets/audio/sfx-master/} for traceability) were converted to WAV via {@code
 * com.damaitaliana.client.buildtools.OggToWavConverter}.
 */
public enum Sfx {
  /** Wooden tap when a piece is placed (impactWood_light_000, ~266 ms). */
  MOVE("/assets/audio/sfx/move.wav"),
  /** Heavier wood thud on capture (impactWood_heavy_000, ~313 ms). */
  CAPTURE("/assets/audio/sfx/capture.wav"),
  /** Glassy bell on promotion (confirmation_002, ~539 ms). */
  PROMOTION("/assets/audio/sfx/promotion.wav"),
  /** Soft buzz on illegal interaction (error_001, ~165 ms). */
  ILLEGAL("/assets/audio/sfx/illegal.wav"),
  /** Pizzicato victory sting on player win (jingles_PIZZI07, ~1.32 s). */
  VICTORY("/assets/audio/sfx/win.wav"),
  /** Pizzicato descending sting on player loss (jingles_PIZZI03, ~1.15 s). */
  DEFEAT("/assets/audio/sfx/lose.wav");

  private final String resourcePath;

  Sfx(String resourcePath) {
    this.resourcePath = resourcePath;
  }

  /** Classpath resource path of the WAV file backing this SFX. Always absolute (starts with /). */
  public String resourcePath() {
    return resourcePath;
  }
}
