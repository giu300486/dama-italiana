/**
 * Audio playback services for SFX and ambient music (SPEC §13.4, ADR-035).
 *
 * <p>Introduced in Fase 3.5: {@link com.damaitaliana.client.audio.AudioService} encapsulates JavaFX
 * {@code MediaPlayer} so controllers stay decoupled from the JavaFX media stack and unit tests can
 * mock the bus. Music ambient plays a random-shuffle playlist with no overlap; SFX fire on gameplay
 * events (move / capture / promotion / illegal / victory / defeat) resolved from {@link
 * com.damaitaliana.client.audio.Sfx}.
 *
 * <p>Volumes are persisted to {@code ~/.dama-italiana/config.json} via the existing {@code
 * PreferencesService} (Task 3.5.4 wiring) and exposed in Settings as 0-100% sliders with
 * independent mute toggles per {@link com.damaitaliana.client.audio.AudioBus}.
 *
 * <p>Task 3.5.3 ships the type skeleton only; {@link
 * com.damaitaliana.client.audio.JavaFxAudioService} is a no-op until Task 3.5.4 wires the {@code
 * MediaPlayer} pool.
 */
package com.damaitaliana.client.audio;
