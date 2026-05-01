package com.damaitaliana.client.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Skeleton {@link AudioService} implementation. Task 3.5.3 ships the no-op shell so the bean is
 * resolvable and existing controllers can declare a dependency; Task 3.5.4 wires the JavaFX {@code
 * MediaPlayer} pool, the {@link MusicPlaylist} for ambient music, and the {@code
 * PreferencesService}-backed volume/mute persistence.
 *
 * <p>While the skeleton is active, every public method returns silently (or returns sensible
 * defaults) and a single DEBUG log line records the call for traceability.
 */
@Component
public class JavaFxAudioService implements AudioService {

  private static final Logger log = LoggerFactory.getLogger(JavaFxAudioService.class);
  private static final int DEFAULT_MUSIC_VOLUME = 30; // SPEC §13.4
  private static final int DEFAULT_SFX_VOLUME = 70; // SPEC §13.4

  @Override
  public void playMusicShuffle() {
    log.debug("playMusicShuffle (skeleton no-op; Task 3.5.4)");
  }

  @Override
  public void stopMusic() {
    log.debug("stopMusic (skeleton no-op; Task 3.5.4)");
  }

  @Override
  public void playSfx(Sfx sfx) {
    log.debug("playSfx({}) (skeleton no-op; Task 3.5.4)", sfx);
  }

  @Override
  public void setVolume(AudioBus bus, int percent) {
    log.debug("setVolume({}, {}) (skeleton no-op; Task 3.5.4)", bus, percent);
  }

  @Override
  public int volume(AudioBus bus) {
    return switch (bus) {
      case MUSIC -> DEFAULT_MUSIC_VOLUME;
      case SFX -> DEFAULT_SFX_VOLUME;
    };
  }

  @Override
  public void setMuted(AudioBus bus, boolean muted) {
    log.debug("setMuted({}, {}) (skeleton no-op; Task 3.5.4)", bus, muted);
  }

  @Override
  public boolean isMuted(AudioBus bus) {
    return false;
  }
}
