package com.damaitaliana.client.audio;

import com.damaitaliana.client.persistence.PreferencesService;
import com.damaitaliana.client.persistence.UserPreferences;
import jakarta.annotation.PreDestroy;
import java.net.URL;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Production {@link AudioService} backed by {@link MediaPlayer}.
 *
 * <p>Two playback paths share this bean, each with its own bus state (volume + mute) persisted to
 * {@code config.json}:
 *
 * <ul>
 *   <li><b>SFX bus</b>: a {@link MediaPlayer} per {@link Sfx} cached on first use; subsequent calls
 *       seek back to {@code Duration.ZERO} and replay. Cheaper than instantiating fresh players per
 *       shot and avoids the GC pressure of per-call {@code Media} construction. Concurrency is
 *       limited by JavaFX (one playback per {@code MediaPlayer}); back-to-back SFX of the same kind
 *       are sequential, but different cues overlap.
 *   <li><b>Music bus</b>: a single {@link MediaPlayer} fed by {@link MusicPlaylist}; on {@code
 *       setOnEndOfMedia} the player is disposed and the next track is built from the playlist. The
 *       playlist resource list is discovered eagerly in the constructor (see {@link
 *       #MUSIC_TRACKS}); missing resources are tolerated — the bus simply remains silent.
 * </ul>
 *
 * <p>All JavaFX media interactions are funnelled through {@link Platform#runLater(Runnable)} so
 * that callers from worker threads do not have to know about the JavaFX threading model.
 *
 * <p>Initial volumes/mutes are loaded from {@link PreferencesService}; every setter persists.
 */
@Component
public class JavaFxAudioService implements AudioService {

  private static final Logger log = LoggerFactory.getLogger(JavaFxAudioService.class);

  /**
   * Music tracks bundled in {@code /assets/audio/music/} per CREDITS.md (4 CC0 ambient tracks).
   * Files missing at runtime are skipped with a WARN log line; if none are present, the music bus
   * stays silent and {@link #playMusicShuffle()} becomes a no-op.
   */
  static final List<String> MUSIC_TRACKS =
      List.of(
          "/assets/audio/music/calm_piano_1.ogg",
          "/assets/audio/music/first_light_particles.ogg",
          "/assets/audio/music/at_home_orchestral.ogg",
          "/assets/audio/music/peaceful_forest.ogg");

  private final PreferencesService preferencesService;
  private final Map<Sfx, MediaPlayer> sfxPlayers = new EnumMap<>(Sfx.class);

  private final Map<AudioBus, Integer> volumes = new EnumMap<>(AudioBus.class);
  private final Map<AudioBus, Boolean> mutes = new EnumMap<>(AudioBus.class);

  private MusicPlaylist playlist;
  private MediaPlayer musicPlayer;
  private boolean musicRequested;

  public JavaFxAudioService(PreferencesService preferencesService) {
    this.preferencesService = Objects.requireNonNull(preferencesService, "preferencesService");
    UserPreferences prefs = preferencesService.load();
    volumes.put(AudioBus.MUSIC, prefs.musicVolumePercent());
    volumes.put(AudioBus.SFX, prefs.sfxVolumePercent());
    mutes.put(AudioBus.MUSIC, prefs.musicMuted());
    mutes.put(AudioBus.SFX, prefs.sfxMuted());
  }

  @Override
  public synchronized void playMusicShuffle() {
    musicRequested = true;
    if (mutes.get(AudioBus.MUSIC)) {
      log.debug("playMusicShuffle skipped — music bus muted");
      return;
    }
    if (playlist == null) {
      playlist = buildPlaylist();
      if (playlist == null) {
        log.warn("Music bus has no playable tracks; playMusicShuffle is a no-op");
        return;
      }
    }
    if (musicPlayer != null
        && (musicPlayer.getStatus() == MediaPlayer.Status.PLAYING
            || musicPlayer.getStatus() == MediaPlayer.Status.PAUSED)) {
      return;
    }
    advanceMusic();
  }

  @Override
  public synchronized void stopMusic() {
    musicRequested = false;
    if (musicPlayer != null) {
      MediaPlayer victim = musicPlayer;
      musicPlayer = null;
      runOnFxThread(victim::dispose);
    }
  }

  @Override
  public void playSfx(Sfx sfx) {
    Objects.requireNonNull(sfx, "sfx");
    if (mutes.get(AudioBus.SFX)) {
      return;
    }
    URL resource = getClass().getResource(sfx.resourcePath());
    if (resource == null) {
      log.warn("SFX resource missing for {} ({}); ignoring", sfx, sfx.resourcePath());
      return;
    }
    runOnFxThread(
        () -> {
          MediaPlayer player =
              sfxPlayers.computeIfAbsent(
                  sfx, k -> newPlayer(resource.toExternalForm(), AudioBus.SFX));
          if (player == null) {
            return;
          }
          player.setVolume(scaledVolume(AudioBus.SFX));
          player.stop();
          player.seek(javafx.util.Duration.ZERO);
          player.play();
        });
  }

  @Override
  public synchronized void setVolume(AudioBus bus, int percent) {
    int clamped = clamp(percent);
    volumes.put(bus, clamped);
    persistAudioPreferences();
    applyMusicVolumeIfActive();
  }

  @Override
  public int volume(AudioBus bus) {
    return volumes.get(bus);
  }

  @Override
  public synchronized void setMuted(AudioBus bus, boolean muted) {
    mutes.put(bus, muted);
    persistAudioPreferences();
    if (bus == AudioBus.MUSIC) {
      if (muted) {
        if (musicPlayer != null) {
          MediaPlayer victim = musicPlayer;
          musicPlayer = null;
          runOnFxThread(victim::dispose);
        }
      } else if (musicRequested) {
        playMusicShuffle();
      }
    }
  }

  @Override
  public boolean isMuted(AudioBus bus) {
    return mutes.get(bus);
  }

  @PreDestroy
  void shutdown() {
    if (musicPlayer != null) {
      try {
        musicPlayer.dispose();
      } catch (RuntimeException ex) {
        log.debug("musicPlayer.dispose() threw during shutdown", ex);
      }
      musicPlayer = null;
    }
    sfxPlayers
        .values()
        .forEach(
            p -> {
              try {
                p.dispose();
              } catch (RuntimeException ex) {
                log.debug("sfxPlayer.dispose() threw during shutdown", ex);
              }
            });
    sfxPlayers.clear();
  }

  // ---- internals ----

  private MusicPlaylist buildPlaylist() {
    List<String> available =
        MUSIC_TRACKS.stream().filter(p -> getClass().getResource(p) != null).toList();
    if (available.isEmpty()) {
      return null;
    }
    if (available.size() < MUSIC_TRACKS.size()) {
      log.info(
          "Music bus discovered {}/{} bundled tracks; missing entries are tolerated",
          available.size(),
          MUSIC_TRACKS.size());
    }
    return new MusicPlaylist(available, new SplittableRandom(System.nanoTime()));
  }

  private void advanceMusic() {
    String track = playlist.next();
    URL url = getClass().getResource(track);
    if (url == null) {
      log.warn("Music track {} disappeared at runtime; skipping", track);
      runOnFxThread(this::tryAdvanceNext);
      return;
    }
    runOnFxThread(
        () -> {
          MediaPlayer player = newPlayer(url.toExternalForm(), AudioBus.MUSIC);
          if (player == null) {
            return;
          }
          player.setOnEndOfMedia(this::tryAdvanceNext);
          musicPlayer = player;
          player.setVolume(scaledVolume(AudioBus.MUSIC));
          player.play();
        });
  }

  private synchronized void tryAdvanceNext() {
    if (!musicRequested || mutes.get(AudioBus.MUSIC) || playlist == null) {
      return;
    }
    if (musicPlayer != null) {
      MediaPlayer old = musicPlayer;
      musicPlayer = null;
      runOnFxThread(old::dispose);
    }
    advanceMusic();
  }

  private MediaPlayer newPlayer(String externalUrl, AudioBus bus) {
    try {
      Media media = new Media(externalUrl);
      MediaPlayer player = new MediaPlayer(media);
      player.setVolume(scaledVolume(bus));
      return player;
    } catch (RuntimeException ex) {
      log.warn("Failed to instantiate MediaPlayer for {} on bus {}", externalUrl, bus, ex);
      return null;
    }
  }

  private double scaledVolume(AudioBus bus) {
    if (mutes.get(bus)) {
      return 0.0;
    }
    return volumes.get(bus) / 100.0;
  }

  private void applyMusicVolumeIfActive() {
    if (musicPlayer != null) {
      MediaPlayer ref = musicPlayer;
      runOnFxThread(() -> ref.setVolume(scaledVolume(AudioBus.MUSIC)));
    }
  }

  private void persistAudioPreferences() {
    UserPreferences current = preferencesService.load();
    UserPreferences updated =
        current
            .withMusicVolumePercent(volumes.get(AudioBus.MUSIC))
            .withSfxVolumePercent(volumes.get(AudioBus.SFX))
            .withMusicMuted(mutes.get(AudioBus.MUSIC))
            .withSfxMuted(mutes.get(AudioBus.SFX));
    try {
      preferencesService.save(updated);
    } catch (RuntimeException ex) {
      log.warn("Failed to persist audio preferences; in-memory state retained", ex);
    }
  }

  private static int clamp(int v) {
    if (v < 0) {
      return 0;
    }
    if (v > 100) {
      return 100;
    }
    return v;
  }

  /**
   * Runs {@code task} on the JavaFX Application Thread; if the toolkit is not initialised (unit
   * tests, headless contexts) the task runs inline so behaviour-only tests can still observe state
   * without booting JavaFX.
   */
  private static void runOnFxThread(Runnable task) {
    try {
      if (Platform.isFxApplicationThread()) {
        task.run();
      } else {
        Platform.runLater(task);
      }
    } catch (IllegalStateException toolkitNotInitialised) {
      task.run();
    }
  }
}
