package com.damaitaliana.client.audio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class MusicPlaylistTest {

  @Test
  void rejectsEmptyTrackList() {
    assertThatThrownBy(() -> new MusicPlaylist(List.of(), new SplittableRandom(1L)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void singleTrackPlaylistAlwaysReturnsTheOnlyTrack() {
    MusicPlaylist playlist = new MusicPlaylist(List.of("a"), new SplittableRandom(1L));
    for (int i = 0; i < 5; i++) {
      assertThat(playlist.next()).isEqualTo("a");
    }
  }

  @Test
  void cyclesThroughEveryTrackBeforeRepeatingWithSeededRng() {
    List<String> tracks = List.of("a", "b", "c", "d");
    MusicPlaylist playlist = new MusicPlaylist(tracks, new SplittableRandom(42L));
    java.util.Set<String> seen = new java.util.HashSet<>();
    for (int i = 0; i < tracks.size(); i++) {
      seen.add(playlist.next());
    }
    assertThat(seen).containsExactlyInAnyOrderElementsOf(tracks);
  }

  @Test
  void avoidsBackToBackRepeatsAcrossReshuffleBoundary() {
    // 4 tracks, deterministic seed; we draw 8 in a row and assert no consecutive duplicates.
    List<String> tracks = List.of("a", "b", "c", "d");
    MusicPlaylist playlist = new MusicPlaylist(tracks, new SplittableRandom(7L));
    String previous = null;
    for (int i = 0; i < 8; i++) {
      String current = playlist.next();
      assertThat(current).as("track at index %d", i).isNotEqualTo(previous);
      previous = current;
    }
  }

  @Test
  void resetForcesAFreshShuffleOnNextNext() {
    MusicPlaylist playlist = new MusicPlaylist(List.of("a", "b", "c"), new SplittableRandom(5L));
    playlist.next();
    playlist.next();
    playlist.reset();
    // After reset the cursor is at the end of the deck, so next() reshuffles immediately.
    assertThat(playlist.next()).isIn("a", "b", "c");
  }

  @Test
  void sizeMatchesTrackCount() {
    assertThat(new MusicPlaylist(List.of("a", "b", "c"), new SplittableRandom(1L)).size())
        .isEqualTo(3);
  }
}
