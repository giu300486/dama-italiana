package com.damaitaliana.client.audio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AudioBusTest {

  @Test
  void exposesMusicAndSfxBuses() {
    assertThat(AudioBus.values()).containsExactly(AudioBus.MUSIC, AudioBus.SFX);
  }
}
