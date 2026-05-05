package com.damaitaliana.core.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.damaitaliana.core.eventbus.MatchEventBus;
import com.damaitaliana.core.match.event.MatchEnded;
import com.damaitaliana.core.match.event.MatchEvent;
import com.damaitaliana.core.match.event.MoveApplied;
import com.damaitaliana.core.match.event.MoveRejected;
import com.damaitaliana.core.repository.inmemory.InMemoryMatchRepository;
import com.damaitaliana.core.stomp.StompCompatiblePublisher;
import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
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
 * Rule-engine integration tests for {@link MatchManager} (A4.6 + terminal handling). Mixes the real
 * {@link ItalianRuleEngine} with Mockito stubs of {@link RuleEngine} for the terminal-state
 * scenario where engineering a real Italian-draughts position that ends in one move would clutter
 * the test without exercising additional MatchManager behaviour.
 */
@ExtendWith(MockitoExtension.class)
class MatchManagerValidationTest {

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

  // --- Real engine: legal + illegal flows -----------------------------------

  @Test
  void legalMoveProducesMoveAppliedAndUpdatesState() {
    MatchManager manager = newManager(new ItalianRuleEngine());
    Match match = manager.createMatch(ALICE, BOB, TC);
    Move legalForWhite = new ItalianRuleEngine().legalMoves(match.state()).get(0);

    MatchEvent event = manager.applyMove(match.id(), ALICE, legalForWhite);

    assertThat(event).isInstanceOf(MoveApplied.class);
    MoveApplied applied = (MoveApplied) event;
    assertThat(applied.move()).isEqualTo(legalForWhite);
    assertThat(applied.sequenceNo()).isZero();
    // State has advanced — sideToMove flipped to BLACK.
    assertThat(match.state().sideToMove()).isEqualTo(Color.BLACK);
    assertThat(match.status()).isEqualTo(MatchStatus.ONGOING);
  }

  @Test
  void legalMoveResetsAntiCheatCounterForSender() {
    MatchManager manager = newManager(new ItalianRuleEngine());
    Match match = manager.createMatch(ALICE, BOB, TC);
    // Bump WHITE counter via two NOT_YOUR_TURN rejections (BOB tried to move when it's WHITE's
    // turn — wait, BOB rejection bumps BLACK counter, not WHITE). Use applyMove from BOB twice.
    Move legalForWhite = new ItalianRuleEngine().legalMoves(match.state()).get(0);
    manager.applyMove(match.id(), BOB, legalForWhite); // bumps BLACK counter to 1
    manager.applyMove(match.id(), BOB, legalForWhite); // bumps BLACK counter to 2
    assertThat(match.consecutiveIllegalMoves(Color.BLACK)).isEqualTo(2);

    // Now WHITE plays a legal move, then BLACK plays a legal move (which resets BLACK's counter).
    manager.applyMove(match.id(), ALICE, legalForWhite); // legal — BLACK counter unchanged
    Move legalForBlack = new ItalianRuleEngine().legalMoves(match.state()).get(0);
    manager.applyMove(match.id(), BOB, legalForBlack);

    assertThat(match.consecutiveIllegalMoves(Color.BLACK)).isZero();
  }

  @Test
  void illegalMoveProducesMoveRejectedAndIncrementsCounter() {
    MatchManager manager = newManager(new ItalianRuleEngine());
    Match match = manager.createMatch(ALICE, BOB, TC);
    Move whiteFirstMove = new ItalianRuleEngine().legalMoves(match.state()).get(0);
    manager.applyMove(match.id(), ALICE, whiteFirstMove);
    Move blackResponse = new ItalianRuleEngine().legalMoves(match.state()).get(0);
    manager.applyMove(match.id(), BOB, blackResponse);

    // White's turn again. Replay the very first move — its from-square is empty now → illegal.
    MatchEvent event = manager.applyMove(match.id(), ALICE, whiteFirstMove);

    assertThat(event).isInstanceOf(MoveRejected.class);
    MoveRejected rejected = (MoveRejected) event;
    assertThat(rejected.reason()).isEqualTo(RejectionReason.ILLEGAL_MOVE);
    assertThat(match.consecutiveIllegalMoves(Color.WHITE)).isEqualTo(1);
    assertThat(match.consecutiveIllegalMoves(Color.BLACK)).isZero();
    // Match still ongoing, side-to-move still WHITE — state unchanged by the rejection.
    assertThat(match.status()).isEqualTo(MatchStatus.ONGOING);
    assertThat(match.state().sideToMove()).isEqualTo(Color.WHITE);
  }

  // --- Stubbed engine: terminal-status handling -----------------------------

  @Test
  void legalMoveLeadingToTerminalStateBroadcastsMatchEnded(@Mock RuleEngine stubEngine) {
    MatchManager manager = newManager(stubEngine);
    Match match = manager.createMatch(ALICE, BOB, TC);
    Move legalForWhite = new ItalianRuleEngine().legalMoves(match.state()).get(0);
    GameState terminal =
        new GameState(Board.initial(), Color.BLACK, 0, List.of(), GameStatus.WHITE_WINS);
    when(stubEngine.applyMove(any(), eq(legalForWhite))).thenReturn(terminal);

    MatchEvent event = manager.applyMove(match.id(), ALICE, legalForWhite);

    assertThat(event).isInstanceOf(MoveApplied.class);
    // Match transitioned to FINISHED; the MatchEnded follow-up event is in the log.
    assertThat(match.status()).isEqualTo(MatchStatus.FINISHED);
    List<MatchEvent> events = manager.eventsSince(match.id(), -1L);
    assertThat(events).hasSize(2);
    assertThat(events.get(1)).isInstanceOf(MatchEnded.class);
    MatchEnded ended = (MatchEnded) events.get(1);
    assertThat(ended.result()).isEqualTo(MatchResult.WHITE_WINS);
    assertThat(ended.reason()).isEqualTo(EndReason.CHECKMATE_LIKE);
  }

  @Test
  void legalMoveLeadingToDrawByRepetitionBroadcastsMatchEndedWithDrawReason(
      @Mock RuleEngine stubEngine) {
    MatchManager manager = newManager(stubEngine);
    Match match = manager.createMatch(ALICE, BOB, TC);
    Move legalForWhite = new ItalianRuleEngine().legalMoves(match.state()).get(0);
    GameState drawState =
        new GameState(Board.initial(), Color.BLACK, 0, List.of(), GameStatus.DRAW_REPETITION);
    when(stubEngine.applyMove(any(), eq(legalForWhite))).thenReturn(drawState);

    manager.applyMove(match.id(), ALICE, legalForWhite);

    List<MatchEvent> events = manager.eventsSince(match.id(), -1L);
    assertThat(events).hasSize(2);
    MatchEnded ended = (MatchEnded) events.get(1);
    assertThat(ended.result()).isEqualTo(MatchResult.DRAW);
    assertThat(ended.reason()).isEqualTo(EndReason.DRAW_REPETITION);
  }

  @Test
  void engineThrowingIllegalMoveExceptionBecomesMoveRejected(@Mock RuleEngine stubEngine) {
    MatchManager manager = newManager(stubEngine);
    Match match = manager.createMatch(ALICE, BOB, TC);
    Move someMove = new ItalianRuleEngine().legalMoves(match.state()).get(0);
    when(stubEngine.applyMove(any(), eq(someMove)))
        .thenThrow(new IllegalMoveException("stubbed illegal"));

    MatchEvent event = manager.applyMove(match.id(), ALICE, someMove);

    assertThat(event).isInstanceOf(MoveRejected.class);
    assertThat(((MoveRejected) event).reason()).isEqualTo(RejectionReason.ILLEGAL_MOVE);
    assertThat(match.consecutiveIllegalMoves(Color.WHITE)).isEqualTo(1);
  }

  // --- Helpers --------------------------------------------------------------

  private MatchManager newManager(RuleEngine engine) {
    return new MatchManager(repo, engine, bus, stompPublisher, CLOCK);
  }
}
