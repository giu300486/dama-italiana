package com.damaitaliana.core.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.core.match.Match;
import com.damaitaliana.core.match.MatchId;
import com.damaitaliana.core.match.MatchStatus;
import com.damaitaliana.core.match.TimeControl;
import com.damaitaliana.core.match.UserRef;
import com.damaitaliana.core.match.event.DrawAccepted;
import com.damaitaliana.core.match.event.DrawDeclined;
import com.damaitaliana.core.match.event.DrawOffered;
import com.damaitaliana.core.match.event.MatchEvent;
import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Abstract contract test for any {@link MatchRepository} implementation. In Fase 4 it is extended
 * by {@code InMemoryMatchRepositoryTest} (Task 4.4); in Fase 6 the JPA adapter test will extend the
 * same contract, ensuring the two implementations behave identically against the documented port
 * semantics.
 *
 * <p>Concrete subclasses provide a fresh repository instance via {@link #createRepository()} —
 * {@link #setUp()} re-creates it before every test. The class is {@code abstract} so JUnit 5's
 * Jupiter engine skips it during discovery.
 */
abstract class MatchRepositoryContractTest {

  protected MatchRepository repo;

  protected static final Instant NOW = Instant.parse("2026-05-03T08:00:00Z");
  protected static final UserRef ALICE = UserRef.anonymousLan("alice");
  protected static final UserRef BOB = UserRef.anonymousLan("bob");
  protected static final UserRef CAROL = UserRef.anonymousLan("carol");

  /** Concrete subclasses return a fresh, empty repository. */
  protected abstract MatchRepository createRepository();

  @BeforeEach
  void setUp() {
    repo = createRepository();
  }

  // --- save + findById ------------------------------------------------------

  @Test
  void findByIdReturnsEmptyWhenNoMatchSaved() {
    assertThat(repo.findById(MatchId.random())).isEmpty();
  }

  @Test
  void saveAndFindByIdRoundtripsTheMatchReference() {
    Match m = newOngoingMatch(MatchId.random(), ALICE, BOB);
    repo.save(m);

    assertThat(repo.findById(m.id())).contains(m);
  }

  @Test
  void saveOverwritesExistingMatchWithSameId() {
    MatchId mid = MatchId.random();
    Match first = newOngoingMatch(mid, ALICE, BOB);
    repo.save(first);

    Match replacement = newOngoingMatch(mid, ALICE, CAROL);
    repo.save(replacement);

    assertThat(repo.findById(mid)).contains(replacement);
  }

  // --- findByStatus ---------------------------------------------------------

  @Test
  void findByStatusReturnsEmptyWhenNoMatchMatches() {
    repo.save(newOngoingMatch(MatchId.random(), ALICE, BOB));
    assertThat(repo.findByStatus(MatchStatus.FINISHED)).isEmpty();
  }

  @Test
  void findByStatusReturnsAllMatchesWithThatStatus() {
    Match a = newOngoingMatch(MatchId.random(), ALICE, BOB);
    Match b = newOngoingMatch(MatchId.random(), ALICE, CAROL);
    Match c = newWaitingMatch(MatchId.random(), BOB);
    repo.save(a);
    repo.save(b);
    repo.save(c);

    assertThat(repo.findByStatus(MatchStatus.ONGOING)).containsExactlyInAnyOrder(a, b);
    assertThat(repo.findByStatus(MatchStatus.WAITING)).containsExactly(c);
  }

  // --- findByPlayer ---------------------------------------------------------

  @Test
  void findByPlayerReturnsMatchesAsWhiteOrBlack() {
    Match aliceWhite = newOngoingMatch(MatchId.random(), ALICE, BOB);
    Match aliceBlack = newOngoingMatch(MatchId.random(), CAROL, ALICE);
    Match noAlice = newOngoingMatch(MatchId.random(), BOB, CAROL);
    repo.save(aliceWhite);
    repo.save(aliceBlack);
    repo.save(noAlice);

    assertThat(repo.findByPlayer(ALICE)).containsExactlyInAnyOrder(aliceWhite, aliceBlack);
    assertThat(repo.findByPlayer(BOB)).containsExactlyInAnyOrder(aliceWhite, noAlice);
    assertThat(repo.findByPlayer(CAROL)).containsExactlyInAnyOrder(aliceBlack, noAlice);
  }

  // --- appendEvent + currentSequenceNo + eventsSince ------------------------

  @Test
  void currentSequenceNoIsMinusOneBeforeAnyEvent() {
    MatchId mid = MatchId.random();
    repo.save(newOngoingMatch(mid, ALICE, BOB));

    assertThat(repo.currentSequenceNo(mid)).isEqualTo(-1L);
    assertThat(repo.eventsSince(mid, -1L)).isEmpty();
  }

  @Test
  void appendEventAdvancesSequenceMonotonically() {
    MatchId mid = MatchId.random();
    repo.save(newOngoingMatch(mid, ALICE, BOB));

    repo.appendEvent(new DrawOffered(mid, 0L, NOW, ALICE));
    assertThat(repo.currentSequenceNo(mid)).isZero();

    repo.appendEvent(new DrawDeclined(mid, 1L, NOW));
    assertThat(repo.currentSequenceNo(mid)).isEqualTo(1L);

    repo.appendEvent(new DrawOffered(mid, 2L, NOW, BOB));
    assertThat(repo.currentSequenceNo(mid)).isEqualTo(2L);
  }

  @Test
  void appendEventRejectsNonMonotonicSequence() {
    MatchId mid = MatchId.random();
    repo.save(newOngoingMatch(mid, ALICE, BOB));
    repo.appendEvent(new DrawOffered(mid, 0L, NOW, ALICE));

    assertThatThrownBy(() -> repo.appendEvent(new DrawDeclined(mid, 2L, NOW)))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> repo.appendEvent(new DrawDeclined(mid, 0L, NOW)))
        .isInstanceOf(IllegalStateException.class);

    // Counter is unchanged after rejected appends.
    assertThat(repo.currentSequenceNo(mid)).isZero();
  }

  @Test
  void appendEventRequiresFirstEventToHaveSequenceZero() {
    MatchId mid = MatchId.random();
    repo.save(newOngoingMatch(mid, ALICE, BOB));

    assertThatThrownBy(() -> repo.appendEvent(new DrawOffered(mid, 5L, NOW, ALICE)))
        .isInstanceOf(IllegalStateException.class);
    assertThat(repo.currentSequenceNo(mid)).isEqualTo(-1L);
  }

  @Test
  void eventsSinceReturnsTheSuffixInOrder() {
    MatchId mid = MatchId.random();
    repo.save(newOngoingMatch(mid, ALICE, BOB));
    DrawOffered e0 = new DrawOffered(mid, 0L, NOW, ALICE);
    DrawDeclined e1 = new DrawDeclined(mid, 1L, NOW);
    DrawAccepted e2 = new DrawAccepted(mid, 2L, NOW);
    repo.appendEvent(e0);
    repo.appendEvent(e1);
    repo.appendEvent(e2);

    List<MatchEvent> all = repo.eventsSince(mid, -1L);
    assertThat(all).containsExactly(e0, e1, e2);

    List<MatchEvent> afterFirst = repo.eventsSince(mid, 0L);
    assertThat(afterFirst).containsExactly(e1, e2);

    List<MatchEvent> afterAll = repo.eventsSince(mid, 2L);
    assertThat(afterAll).isEmpty();
  }

  @Test
  void eventsSinceReturnsEmptyForUnknownMatch() {
    assertThat(repo.eventsSince(MatchId.random(), -1L)).isEmpty();
  }

  @Test
  void currentSequenceNoIsMinusOneForUnknownMatch() {
    assertThat(repo.currentSequenceNo(MatchId.random())).isEqualTo(-1L);
  }

  // --- fixtures -------------------------------------------------------------

  private Match newOngoingMatch(MatchId id, UserRef white, UserRef black) {
    GameState initial =
        new GameState(Board.initial(), Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    return new Match(
        id,
        white,
        black,
        TimeControl.unlimited(),
        NOW,
        null,
        initial,
        -1L,
        MatchStatus.ONGOING,
        null);
  }

  private Match newWaitingMatch(MatchId id, UserRef white) {
    GameState initial =
        new GameState(Board.initial(), Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    return new Match(
        id,
        white,
        white, // single-player placeholder; second player joins later
        TimeControl.unlimited(),
        NOW,
        null,
        initial,
        -1L,
        MatchStatus.WAITING,
        null);
  }
}
