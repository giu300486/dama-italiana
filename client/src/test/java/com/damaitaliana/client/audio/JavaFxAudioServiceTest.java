package com.damaitaliana.client.audio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JavaFxAudioServiceTest {

  private final JavaFxAudioService service = new JavaFxAudioService();

  @Test
  void skeletonReturnsSpecDefaultVolumePerBus() {
    // SPEC §13.4: defaults are 30 (music) and 70 (sfx); the skeleton honours them so callers
    // wired before Task 3.5.4 see sensible values.
    assertThat(service.volume(AudioBus.MUSIC)).isEqualTo(30);
    assertThat(service.volume(AudioBus.SFX)).isEqualTo(70);
  }

  @Test
  void skeletonReportsBothBusesUnmuted() {
    assertThat(service.isMuted(AudioBus.MUSIC)).isFalse();
    assertThat(service.isMuted(AudioBus.SFX)).isFalse();
  }

  @Test
  void skeletonNoOpMethodsDoNotThrow() {
    // playMusic / stopMusic / playSfx / setVolume / setMuted log a debug line and return.
    service.playMusicShuffle();
    service.stopMusic();
    service.playSfx(Sfx.MOVE);
    service.setVolume(AudioBus.MUSIC, 50);
    service.setMuted(AudioBus.SFX, true);
  }
}
