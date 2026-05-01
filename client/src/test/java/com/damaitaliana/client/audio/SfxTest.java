package com.damaitaliana.client.audio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SfxTest {

  @Test
  void exposesSixGameplayCues() {
    assertThat(Sfx.values())
        .containsExactly(
            Sfx.MOVE, Sfx.CAPTURE, Sfx.PROMOTION, Sfx.ILLEGAL, Sfx.VICTORY, Sfx.DEFEAT);
  }

  @Test
  void everyValueExposesAClasspathResourcePath() {
    for (Sfx sfx : Sfx.values()) {
      assertThat(sfx.resourcePath())
          .as("resource path of %s", sfx)
          .startsWith("/assets/audio/sfx/")
          .endsWith(".ogg");
    }
  }

  @Test
  void resourcePathsAreUnique() {
    long distinct = java.util.Arrays.stream(Sfx.values()).map(Sfx::resourcePath).distinct().count();
    assertThat(distinct).isEqualTo(Sfx.values().length);
  }

  @Test
  void everyResourceIsBundledOnTheClasspath() {
    // SFX assets were committed in Task 3.5.1; the enum must point at real files.
    for (Sfx sfx : Sfx.values()) {
      assertThat(getClass().getResource(sfx.resourcePath()))
          .as("classpath resource for %s", sfx)
          .isNotNull();
    }
  }
}
