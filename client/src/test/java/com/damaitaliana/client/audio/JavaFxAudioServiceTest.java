package com.damaitaliana.client.audio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.damaitaliana.client.persistence.PreferencesService;
import com.damaitaliana.client.persistence.UserPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JavaFxAudioServiceTest {

  private PreferencesService preferencesService;
  private JavaFxAudioService service;

  @BeforeEach
  void setUp() {
    preferencesService = mock(PreferencesService.class);
    when(preferencesService.load()).thenReturn(UserPreferences.defaults());
    service = new JavaFxAudioService(preferencesService);
  }

  @Test
  void initialisesFromPersistedPreferences() {
    when(preferencesService.load())
        .thenReturn(UserPreferences.defaults().withMusicVolumePercent(45).withSfxMuted(true));
    JavaFxAudioService configured = new JavaFxAudioService(preferencesService);

    assertThat(configured.volume(AudioBus.MUSIC)).isEqualTo(45);
    assertThat(configured.isMuted(AudioBus.SFX)).isTrue();
  }

  @Test
  void defaultsMatchSpec13_4() {
    // SPEC §13.4: defaults 30/70/false/false.
    assertThat(service.volume(AudioBus.MUSIC)).isEqualTo(30);
    assertThat(service.volume(AudioBus.SFX)).isEqualTo(70);
    assertThat(service.isMuted(AudioBus.MUSIC)).isFalse();
    assertThat(service.isMuted(AudioBus.SFX)).isFalse();
  }

  @Test
  void setVolumeClampsAndPersists() {
    service.setVolume(AudioBus.SFX, 250);
    assertThat(service.volume(AudioBus.SFX)).isEqualTo(100);

    service.setVolume(AudioBus.MUSIC, -10);
    assertThat(service.volume(AudioBus.MUSIC)).isEqualTo(0);

    ArgumentCaptor<UserPreferences> captor = ArgumentCaptor.forClass(UserPreferences.class);
    verify(preferencesService, atLeastOnce()).save(captor.capture());
    UserPreferences last = captor.getValue();
    assertThat(last.sfxVolumePercent()).isEqualTo(100);
    assertThat(last.musicVolumePercent()).isEqualTo(0);
  }

  @Test
  void setMutedTogglesAndPersists() {
    service.setMuted(AudioBus.MUSIC, true);
    assertThat(service.isMuted(AudioBus.MUSIC)).isTrue();

    service.setMuted(AudioBus.MUSIC, false);
    assertThat(service.isMuted(AudioBus.MUSIC)).isFalse();

    verify(preferencesService, times(2)).save(any(UserPreferences.class));
  }

  @Test
  void persistFailureIsSwallowedSoUiKeepsCurrentInMemoryState() {
    // PreferencesService.save throwing must not propagate — the user can still toggle audio for
    // this session even if disk I/O is broken.
    org.mockito.Mockito.doThrow(new RuntimeException("disk full"))
        .when(preferencesService)
        .save(any(UserPreferences.class));

    service.setVolume(AudioBus.MUSIC, 55); // does not throw
    assertThat(service.volume(AudioBus.MUSIC)).isEqualTo(55);
  }

  @Test
  void mutingMusicWhilePlayingDoesNotThrow() {
    // Without booting JavaFX, calling playMusicShuffle is a tolerated no-op (no playable tracks
    // visible from the test classpath). Toggling mute afterwards must remain safe.
    service.playMusicShuffle();
    service.setMuted(AudioBus.MUSIC, true);
    service.setMuted(AudioBus.MUSIC, false);
  }

  @Test
  void stopMusicIsIdempotentBeforeAnyPlay() {
    service.stopMusic();
    service.stopMusic();
  }
}
