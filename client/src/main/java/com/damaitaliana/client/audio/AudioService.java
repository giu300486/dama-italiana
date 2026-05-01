package com.damaitaliana.client.audio;

/**
 * Sound playback abstraction. Decouples controllers from the JavaFX {@code MediaPlayer} stack so
 * unit tests can mock the bus and verify dispatch without booting the toolkit.
 *
 * <p>Two independent buses (see {@link AudioBus}): ambient music (single-track, looping playlist)
 * and SFX (one-shot, may overlap). Each has its own volume in the closed range [0, 100] and a mute
 * toggle persisted to {@code config.json} via the existing {@code PreferencesService}.
 *
 * <p>Production wiring: {@link JavaFxAudioService} (Task 3.5.4 fills in the actual playback).
 */
public interface AudioService {

  /**
   * Starts (or restarts) the ambient music playlist in random shuffle, looping continuously. No-op
   * if music is muted or already playing the current playlist.
   */
  void playMusicShuffle();

  /** Stops the ambient music playback if active; safe to call when nothing is playing. */
  void stopMusic();

  /** Plays a one-shot SFX without affecting ambient music. No-op if SFX is muted. */
  void playSfx(Sfx sfx);

  /** Sets the volume of the given bus. Values are clamped to [0, 100]. */
  void setVolume(AudioBus bus, int percent);

  /** Returns the current volume of the given bus in [0, 100]. */
  int volume(AudioBus bus);

  /** Mutes or unmutes the given bus; mute is independent from volume. */
  void setMuted(AudioBus bus, boolean muted);

  /** Whether the given bus is currently muted. */
  boolean isMuted(AudioBus bus);
}
