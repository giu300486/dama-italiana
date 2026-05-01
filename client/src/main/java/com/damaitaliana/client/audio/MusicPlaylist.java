package com.damaitaliana.client.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Random-shuffle playlist over a fixed list of music resource paths. {@link #next()} returns the
 * upcoming track and rotates internally; when the deck is exhausted it reshuffles and starts over,
 * yielding a continuous loop without overlap (SPEC §13.4 "loop continuo a fine playlist").
 *
 * <p>Caller-injectable {@link RandomGenerator} keeps the shuffle deterministic in tests; production
 * wiring (Task 3.5.4) seeds a {@code SplittableRandom(System.nanoTime())}.
 */
public final class MusicPlaylist {

  private final List<String> tracks;
  private final RandomGenerator rng;
  private final List<String> deck;
  private int cursor;

  public MusicPlaylist(List<String> tracks, RandomGenerator rng) {
    Objects.requireNonNull(tracks, "tracks");
    Objects.requireNonNull(rng, "rng");
    if (tracks.isEmpty()) {
      throw new IllegalArgumentException("tracks must not be empty");
    }
    this.tracks = List.copyOf(tracks);
    this.rng = rng;
    this.deck = new ArrayList<>(this.tracks);
    this.cursor = this.deck.size(); // forces a reshuffle on first next()
  }

  /**
   * Returns the next track resource path. Reshuffles transparently when the current deck has been
   * exhausted, ensuring the same track is never replayed twice in a row unless the playlist has
   * exactly one entry.
   */
  public synchronized String next() {
    if (cursor >= deck.size()) {
      reshuffle();
    }
    return deck.get(cursor++);
  }

  /** Resets the playlist to a fresh shuffle; the next {@link #next()} returns a new first track. */
  public synchronized void reset() {
    cursor = deck.size();
  }

  /** Number of distinct tracks. */
  public int size() {
    return tracks.size();
  }

  private void reshuffle() {
    String previousLast = deck.isEmpty() ? null : deck.get(deck.size() - 1);
    Collections.shuffle(deck, asJavaUtilRandom(rng));
    // Avoid back-to-back repeats across reshuffle boundaries by swapping if needed.
    if (deck.size() > 1 && Objects.equals(deck.get(0), previousLast)) {
      String swap = deck.get(0);
      deck.set(0, deck.get(1));
      deck.set(1, swap);
    }
    cursor = 0;
  }

  // RandomGenerator -> java.util.Random adapter for Collections.shuffle.
  private static java.util.Random asJavaUtilRandom(RandomGenerator gen) {
    return new java.util.Random() {
      @Override
      public int nextInt(int bound) {
        return gen.nextInt(bound);
      }

      @Override
      public long nextLong() {
        return gen.nextLong();
      }
    };
  }
}
