package com.damaitaliana.core.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.core.match.Match;
import com.damaitaliana.core.match.MatchId;
import com.damaitaliana.core.match.MatchStatus;
import com.damaitaliana.core.match.TimeControl;
import com.damaitaliana.core.match.UserRef;
import com.damaitaliana.core.match.event.DrawOffered;
import com.damaitaliana.core.match.event.MatchEvent;
import com.damaitaliana.core.repository.inmemory.InMemoryMatchRepository;
import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Concurrency stress test for {@link InMemoryMatchRepository} — covers acceptance criterion A4.12
 * of PLAN-fase-4: 16 threads driving 1000 match creations + 10000 event appends parallel must not
 * corrupt internal state nor break the strict monotonic sequence invariant (FR-COM-04, SPEC §7.5).
 *
 * <p>Tagged {@code slow} so the regular {@code mvn -pl core-server verify -DexcludedGroups=slow,
 * performance} fast loop skips it; root {@code mvn clean verify} (no excludedGroups) at fase
 * closure exercises it.
 *
 * <p>The repository documents per-match write serialization as the caller's responsibility (Javadoc
 * of {@link InMemoryMatchRepository}; the production caller is {@code MatchManager} which holds a
 * per-match lock around every write op). The "different matches" test reproduces that pattern with
 * a per-match {@link Object} held by the test driver. The "same match" test bonus-verifies that
 * under heavy single-key contention with caller-side lock, the {@code synchronized (log)} block
 * inside {@link InMemoryMatchRepository#appendEvent} serializes correctly without dropping or
 * reordering events.
 */
@Tag("slow")
class InMemoryRepositoryConcurrencyTest {

  private static final int THREAD_COUNT = 16;
  private static final int TOTAL_MATCHES = 1000;
  private static final int TOTAL_MOVES = 10_000;
  private static final int MATCHES_FOR_MOVES = 64;
  private static final int SAME_MATCH_APPENDS = 1000;

  private static final UserRef ALICE = UserRef.anonymousLan("alice");
  private static final UserRef BOB = UserRef.anonymousLan("bob");
  private static final Instant NOW = Instant.parse("2026-05-06T08:00:00Z");

  private InMemoryMatchRepository repo;

  @BeforeEach
  void setUp() {
    repo = new InMemoryMatchRepository();
  }

  @Test
  void manyMatchesCreatedConcurrentlyDoNotCorruptRepository() throws InterruptedException {
    List<MatchId> allIds = Collections.synchronizedList(new ArrayList<>(TOTAL_MATCHES));
    List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
    AtomicInteger scheduled = new AtomicInteger();

    runConcurrently(
        THREAD_COUNT,
        60,
        threadIdx -> {
          while (true) {
            int idx = scheduled.getAndIncrement();
            if (idx >= TOTAL_MATCHES) {
              return;
            }
            MatchId id = MatchId.random();
            repo.save(newOngoing(id));
            allIds.add(id);
          }
        },
        failures);

    assertThat(failures).isEmpty();
    assertThat(allIds).hasSize(TOTAL_MATCHES);
    assertThat(new HashSet<>(allIds)).hasSize(TOTAL_MATCHES);
    for (MatchId id : allIds) {
      assertThat(repo.findById(id)).isPresent();
    }
    // findByStatus must iterate the snapshot map without ConcurrentModificationException.
    assertThat(repo.findByStatus(MatchStatus.ONGOING)).hasSize(TOTAL_MATCHES);
    assertThat(repo.findByPlayer(ALICE)).hasSize(TOTAL_MATCHES);
  }

  @Test
  void manyAppendsOnDifferentMatchesPreserveMonotonicSequencePerMatch()
      throws InterruptedException {
    List<MatchId> matchIds = new ArrayList<>(MATCHES_FOR_MOVES);
    for (int i = 0; i < MATCHES_FOR_MOVES; i++) {
      MatchId id = MatchId.random();
      repo.save(newOngoing(id));
      matchIds.add(id);
    }
    Map<MatchId, Object> matchLocks = new ConcurrentHashMap<>();
    for (MatchId id : matchIds) {
      matchLocks.put(id, new Object());
    }

    List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
    AtomicInteger scheduled = new AtomicInteger();
    AtomicInteger appended = new AtomicInteger();

    runConcurrently(
        THREAD_COUNT,
        120,
        threadIdx -> {
          Random rng = new Random(threadIdx * 1009L + 7L);
          while (true) {
            int idx = scheduled.getAndIncrement();
            if (idx >= TOTAL_MOVES) {
              return;
            }
            MatchId id = matchIds.get(rng.nextInt(matchIds.size()));
            Object lock = matchLocks.get(id);
            synchronized (lock) {
              long expected = repo.currentSequenceNo(id) + 1L;
              repo.appendEvent(new DrawOffered(id, expected, NOW, ALICE));
              appended.incrementAndGet();
            }
          }
        },
        failures);

    assertThat(failures).isEmpty();
    assertThat(appended.get()).isEqualTo(TOTAL_MOVES);

    // Each match's log is strictly monotonic from 0; sum across all matches == TOTAL_MOVES.
    int totalEvents = 0;
    for (MatchId id : matchIds) {
      List<MatchEvent> log = repo.eventsSince(id, -1L);
      totalEvents += log.size();
      assertThat(repo.currentSequenceNo(id)).isEqualTo((long) log.size() - 1L);
      for (int i = 0; i < log.size(); i++) {
        assertThat(log.get(i).sequenceNo())
            .as("match %s event #%d sequenceNo", id, i)
            .isEqualTo((long) i);
      }
    }
    assertThat(totalEvents).isEqualTo(TOTAL_MOVES);
  }

  @Test
  void manyAppendsOnSameMatchUnderCallerLockPreserveMonotonicSequence()
      throws InterruptedException {
    MatchId mid = MatchId.random();
    repo.save(newOngoing(mid));
    Object lock = new Object();
    List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
    AtomicInteger scheduled = new AtomicInteger();

    runConcurrently(
        THREAD_COUNT,
        60,
        threadIdx -> {
          while (true) {
            int idx = scheduled.getAndIncrement();
            if (idx >= SAME_MATCH_APPENDS) {
              return;
            }
            synchronized (lock) {
              long expected = repo.currentSequenceNo(mid) + 1L;
              repo.appendEvent(new DrawOffered(mid, expected, NOW, ALICE));
            }
          }
        },
        failures);

    assertThat(failures).isEmpty();
    List<MatchEvent> log = repo.eventsSince(mid, -1L);
    assertThat(log).hasSize(SAME_MATCH_APPENDS);
    for (int i = 0; i < SAME_MATCH_APPENDS; i++) {
      assertThat(log.get(i).sequenceNo()).isEqualTo((long) i);
    }
    assertThat(repo.currentSequenceNo(mid)).isEqualTo((long) SAME_MATCH_APPENDS - 1L);
  }

  // --- helpers --------------------------------------------------------------

  private static Match newOngoing(MatchId id) {
    GameState initial =
        new GameState(Board.initial(), Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    return new Match(
        id,
        ALICE,
        BOB,
        TimeControl.unlimited(),
        NOW,
        null,
        initial,
        -1L,
        MatchStatus.ONGOING,
        null);
  }

  /**
   * Spawns {@code threads} workers, all starting at the same {@link CountDownLatch} gate so the
   * race begins fairly. Captures any {@link Throwable} into {@code failures} (the test asserts the
   * list is empty afterwards). Fails the test if the workers do not finish within {@code
   * timeoutSeconds}.
   */
  @FunctionalInterface
  private interface Worker {
    void run(int threadIdx) throws Exception;
  }

  private static void runConcurrently(
      int threads, long timeoutSeconds, Worker worker, List<Throwable> failures)
      throws InterruptedException {
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch startGate = new CountDownLatch(1);
    CountDownLatch doneGate = new CountDownLatch(threads);
    try {
      for (int t = 0; t < threads; t++) {
        final int threadIdx = t;
        pool.submit(
            () -> {
              try {
                startGate.await();
                worker.run(threadIdx);
              } catch (Throwable th) {
                failures.add(th);
              } finally {
                doneGate.countDown();
              }
            });
      }
      startGate.countDown();
      boolean finished = doneGate.await(timeoutSeconds, TimeUnit.SECONDS);
      assertThat(finished).as("workers must finish within %ds", timeoutSeconds).isTrue();
    } finally {
      pool.shutdownNow();
    }
  }
}
