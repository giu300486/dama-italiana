package com.damaitaliana.core.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.damaitaliana.core.eventbus.MatchEventBus;
import com.damaitaliana.core.match.event.MatchEnded;
import com.damaitaliana.core.match.event.MatchEvent;
import com.damaitaliana.core.match.event.MoveRejected;
import com.damaitaliana.core.repository.inmemory.InMemoryMatchRepository;
import com.damaitaliana.core.stomp.StompCompatiblePublisher;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.IllegalMoveException;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * End-to-end anti-cheat tests via {@link MatchManager} (A4.4 — FR-COM-01, SPEC §9.8.3, ADR-040).
 * Verifies that 5 consecutive illegal-move attempts forfeit the offending player, that a single
 * legal move resets the per-player counter, and that the two players keep independent counters.
 *
 * <p>The third scenario (per-player independence) requires interleaving illegal moves between white
 * and black without either of them making a legal move that would reset their own counter. That is
 * impossible with the real Italian engine because side-to-move flips on every legal move; the test
 * therefore stubs {@link RuleEngine#applyMove} to always throw {@link IllegalMoveException},
 * leaving side-to-move unchanged so both players can keep racking up rejections without flipping
 * turns. The first two scenarios use the real {@link ItalianRuleEngine} since they only need
 * NOT_YOUR_TURN sequences with one or two real legal moves between.
 */
@ExtendWith(MockitoExtension.class)
class AntiCheatTest {

  private static final Instant NOW = Instant.parse("2026-05-05T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final UserRef ALICE = UserRef.anonymousLan("alice");
  private static final UserRef BOB = UserRef.anonymousLan("bob");
  private static final TimeControl TC = TimeControl.unlimited();

  @Mock MatchEventBus bus;
  @Mock StompCompatiblePublisher stompPublisher;

  private InMemoryMatchRepository repo;

  @BeforeEach
  void setUp() {
    repo = new InMemoryMatchRepository();
  }

  // --- 1: forfeit on the 5th consecutive rejection -------------------------

  @Test
  void fiveConsecutiveIllegalMovesForfeitsThePlayerAndAwardsTheOpponent() {
    MatchManager manager = newManager(new ItalianRuleEngine());
    Match match = manager.createMatch(ALICE, BOB, TC);

    // White plays one legal move so it becomes black's turn — every subsequent move WHITE attempts
    // is rejected as NOT_YOUR_TURN and bumps WHITE's counter.
    Move whiteFirst = new ItalianRuleEngine().legalMoves(match.state()).get(0);
    manager.applyMove(match.id(), ALICE, whiteFirst);
    assertThat(match.consecutiveIllegalMoves(Color.WHITE)).isZero();

    for (int attempt = 1; attempt <= 5; attempt++) {
      manager.applyMove(match.id(), ALICE, whiteFirst);
    }

    assertThat(match.status()).isEqualTo(MatchStatus.FINISHED);
    assertThat(match.consecutiveIllegalMoves(Color.WHITE)).isEqualTo(5);
    assertThat(match.consecutiveIllegalMoves(Color.BLACK)).isZero();

    List<MatchEvent> events = manager.eventsSince(match.id(), -1L);
    assertThat(events).hasSize(7); // 1 MoveApplied + 5 MoveRejected + 1 MatchEnded
    assertThat(events.get(0)).isInstanceOf(com.damaitaliana.core.match.event.MoveApplied.class);
    for (int i = 1; i <= 5; i++) {
      MoveRejected rejected = (MoveRejected) events.get(i);
      assertThat(rejected.reason()).isEqualTo(RejectionReason.NOT_YOUR_TURN);
      assertThat(rejected.sender()).isEqualTo(ALICE);
    }
    MatchEnded ended = (MatchEnded) events.get(6);
    assertThat(ended.result()).isEqualTo(MatchResult.BLACK_WINS);
    assertThat(ended.reason()).isEqualTo(EndReason.FORFEIT_ANTI_CHEAT);
  }

  // --- 2: a single legal move clears the counter ---------------------------

  @Test
  void aLegalMoveResetsTheCounterAndAvoidsForfeit() {
    MatchManager manager = newManager(new ItalianRuleEngine());
    Match match = manager.createMatch(ALICE, BOB, TC);

    Move whiteFirst = new ItalianRuleEngine().legalMoves(match.state()).get(0);
    manager.applyMove(match.id(), ALICE, whiteFirst); // legal — black's turn now

    // 4 NOT_YOUR_TURN rejections while it's black's turn.
    for (int i = 0; i < 4; i++) {
      manager.applyMove(match.id(), ALICE, whiteFirst);
    }
    assertThat(match.consecutiveIllegalMoves(Color.WHITE)).isEqualTo(4);
    assertThat(match.status()).isEqualTo(MatchStatus.ONGOING);

    // Black plays a legal move → white's turn again.
    Move blackResponse = new ItalianRuleEngine().legalMoves(match.state()).get(0);
    manager.applyMove(match.id(), BOB, blackResponse);

    // White plays a legal move → counter resets.
    Move whiteSecond = new ItalianRuleEngine().legalMoves(match.state()).get(0);
    manager.applyMove(match.id(), ALICE, whiteSecond);
    assertThat(match.consecutiveIllegalMoves(Color.WHITE)).isZero();

    // Black's turn again. White attempts 4 more illegal moves — counter goes 0 → 4, no forfeit.
    for (int i = 0; i < 4; i++) {
      manager.applyMove(match.id(), ALICE, whiteSecond);
    }
    assertThat(match.consecutiveIllegalMoves(Color.WHITE)).isEqualTo(4);
    assertThat(match.status()).isEqualTo(MatchStatus.ONGOING);
  }

  // --- 3: each player keeps an independent counter -------------------------

  @Test
  void eachPlayerHasItsOwnCounterAndOnlyTheOffenderForfeits(@Mock RuleEngine stubEngine) {
    // Stub the engine so every applyMove throws → ILLEGAL_MOVE rejections without changing turn.
    when(stubEngine.applyMove(any(), any())).thenThrow(new IllegalMoveException("stubbed"));
    MatchManager manager = newManager(stubEngine);
    Match match = manager.createMatch(ALICE, BOB, TC);
    Move anyMove = new ItalianRuleEngine().legalMoves(match.state()).get(0);

    // White attempts 4 illegal moves while it IS white's turn → 4 ILLEGAL_MOVE rejections.
    // White counter: 0 → 4. Black counter: 0.
    for (int i = 0; i < 4; i++) {
      manager.applyMove(match.id(), ALICE, anyMove);
    }
    assertThat(match.consecutiveIllegalMoves(Color.WHITE)).isEqualTo(4);
    assertThat(match.consecutiveIllegalMoves(Color.BLACK)).isZero();

    // Black attempts 4 moves while it's still white's turn → 4 NOT_YOUR_TURN rejections.
    // White counter: still 4. Black counter: 0 → 4.
    for (int i = 0; i < 4; i++) {
      manager.applyMove(match.id(), BOB, anyMove);
    }
    assertThat(match.consecutiveIllegalMoves(Color.WHITE)).isEqualTo(4);
    assertThat(match.consecutiveIllegalMoves(Color.BLACK)).isEqualTo(4);
    assertThat(match.status()).isEqualTo(MatchStatus.ONGOING);

    // White attempts 1 more illegal — counter hits 5 → forfeit white, black wins.
    manager.applyMove(match.id(), ALICE, anyMove);
    assertThat(match.consecutiveIllegalMoves(Color.WHITE)).isEqualTo(5);
    assertThat(match.consecutiveIllegalMoves(Color.BLACK)).isEqualTo(4);
    assertThat(match.status()).isEqualTo(MatchStatus.FINISHED);

    MatchEvent last =
        manager.eventsSince(match.id(), -1L).get(manager.eventsSince(match.id(), -1L).size() - 1);
    MatchEnded ended = (MatchEnded) last;
    assertThat(ended.result()).isEqualTo(MatchResult.BLACK_WINS);
    assertThat(ended.reason()).isEqualTo(EndReason.FORFEIT_ANTI_CHEAT);
  }

  // --- helpers -------------------------------------------------------------

  private MatchManager newManager(RuleEngine engine) {
    return new MatchManager(repo, engine, bus, stompPublisher, CLOCK);
  }
}
