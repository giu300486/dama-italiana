package com.damaitaliana.client.audio;

/**
 * Catalog of one-shot sound effects fired from gameplay events. Each value names the bundled
 * resource under {@code /assets/audio/sfx/} (Task 3.5.1, all Kenney CC0 1.0 — see CREDITS.md).
 *
 * <p>The PLAN-fase-3.5 §3.5.3 originally listed four values (MOVE, CAPTURE, PROMOTION, VICTORY);
 * Task 3.5.1 acquired six because {@code illegal} and {@code lose} are also useful gameplay cues
 * and the assets are CC0. The deviation is documented in {@code AI_CONTEXT.md}.
 */
public enum Sfx {
  /** Wooden tap when a piece is placed (impactWood_light_000.ogg, ~266 ms). */
  MOVE("/assets/audio/sfx/move.ogg"),
  /** Heavier wood thud on capture (impactWood_heavy_000.ogg, ~313 ms). */
  CAPTURE("/assets/audio/sfx/capture.ogg"),
  /** Glassy bell on promotion (confirmation_002.ogg, ~539 ms). */
  PROMOTION("/assets/audio/sfx/promotion.ogg"),
  /** Soft buzz on illegal interaction (error_001.ogg, ~165 ms). */
  ILLEGAL("/assets/audio/sfx/illegal.ogg"),
  /** Pizzicato victory sting on player win (jingles_PIZZI07.ogg, ~1.32 s). */
  VICTORY("/assets/audio/sfx/win.ogg"),
  /** Pizzicato descending sting on player loss (jingles_PIZZI03.ogg, ~1.15 s). */
  DEFEAT("/assets/audio/sfx/lose.ogg");

  private final String resourcePath;

  Sfx(String resourcePath) {
    this.resourcePath = resourcePath;
  }

  /** Classpath resource path of the OGG file backing this SFX. Always absolute (starts with /). */
  public String resourcePath() {
    return resourcePath;
  }
}
