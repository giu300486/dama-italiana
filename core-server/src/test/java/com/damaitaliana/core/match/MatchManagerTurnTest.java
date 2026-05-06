package com.damaitaliana.core.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.damaitaliana.core.eventbus.MatchEventBus;
import com.damaitaliana.core.match.event.MoveRejected;
import com.damaitaliana.core.repository.inmemory.InMemoryMatchRepository;
import com.damaitaliana.core.stomp.StompCompatiblePublisher;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Turn-validation + lookup tests for {@link MatchManager} (A4.5, partially A4.6). Uses a real
 * {@link ItalianRuleEngine} and a real {@link InMemoryMatchRepository} to exercise the production
 * code paths; mocks {@link MatchEventBus} and {@link StompCompatiblePublisher} to assert that each
 * event reaches both broadcast targets exactly once.
 */
@ExtendWith(MockitoExtension.class)
class MatchManagerTurnTest {

  private static final Instant NOW = Instant.parse("2026-05-05T12:00:00Z");
  private static final UserRef ALICE = UserRef.anonymousLan("alice");
  private static final UserRef BOB = UserRef.anonymousLan("bob");
  private static final UserRef CAROL = UserRef.anonymousLan("carol");
  private static final TimeControl TC = TimeControl.unlimited();

  @Mock MatchEventBus bus;
  @Mock StompCompatiblePublisher stompPublisher;

  private InMemoryMatchRepository repo;
  private MatchManager manager;

  @BeforeEach
  void setUp() {
    repo = new InMemoryMatchRepository();
    manager =
        new MatchManager(
            repo, new ItalianRuleEngine(), bus, stompPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void findByIdReturnsEmptyForUnknownMatch() {
    assertThat(manager.findById(MatchId.random())).isEmpty();
  }

  @Test
  void createMatchPersistsMatchAsOngoingWithInitialState() {
    Match match = manager.createMatch(ALICE, BOB, TC);

    assertThat(match.status()).isEqualTo(MatchStatus.ONGOING);
    assertThat(match.white()).isEqualTo(ALICE);
    assertThat(match.black()).isEqualTo(BOB);
    assertThat(match.startedAt()).isEqualTo(NOW);
    assertThat(match.state().sideToMove()).isEqualTo(Color.WHITE);
    assertThat(manager.findById(match.id())).contains(match);
  }

  @Test
  void applyMoveOnUnknownMatchThrowsMatchNotFound() {
    Move someMove =
        new ItalianRuleEngine()
            .legalMoves(com.damaitaliana.shared.domain.GameState.initial())
            .get(0);
    assertThatExceptionOfType(MatchNotFoundException.class)
        .isThrownBy(() -> manager.applyMove(MatchId.random(), ALICE, someMove));
  }

  @Test
  void applyMoveByNonParticipantThrowsIllegalArgument() {
    Match match = manager.createMatch(ALICE, BOB, TC);
    Move legal = new ItalianRuleEngine().legalMoves(match.state()).get(0);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> manager.applyMove(match.id(), CAROL, legal))
        .withMessageContaining("not a participant");
  }

  @Test
  void applyMoveByWrongTurnPlayerProducesNotYourTurnRejection() {
    Match match = manager.createMatch(ALICE, BOB, TC);
    Move legalForWhite = new ItalianRuleEngine().legalMoves(match.state()).get(0);

    MoveRejected rejected = (MoveRejected) manager.applyMove(match.id(), BOB, legalForWhite);

    assertThat(rejected.reason()).isEqualTo(RejectionReason.NOT_YOUR_TURN);
    assertThat(rejected.sender()).isEqualTo(BOB);
    // Counter for sender color (BLACK) bumped; opposite color untouched.
    assertThat(match.consecutiveIllegalMoves(Color.BLACK)).isEqualTo(1);
    assertThat(match.consecutiveIllegalMoves(Color.WHITE)).isZero();
    // Match still ongoing, sideToMove still WHITE — state unaffected.
    assertThat(match.status()).isEqualTo(MatchStatus.ONGOING);
    assertThat(match.state().sideToMove()).isEqualTo(Color.WHITE);
  }

  @Test
  void applyMoveOnFinishedMatchProducesMatchNotOngoingRejectionWithoutCounterBump() {
    Match match = manager.createMatch(ALICE, BOB, TC);
    manager.resign(match.id(), ALICE); // finishes the match

    Move someMove =
        new ItalianRuleEngine()
            .legalMoves(com.damaitaliana.shared.domain.GameState.initial())
            .get(0);
    MoveRejected rejected = (MoveRejected) manager.applyMove(match.id(), BOB, someMove);

    assertThat(rejected.reason()).isEqualTo(RejectionReason.MATCH_NOT_ONGOING);
    assertThat(match.consecutiveIllegalMoves(Color.BLACK)).isZero();
    assertThat(match.consecutiveIllegalMoves(Color.WHITE)).isZero();
  }

  @Test
  void everyEmittedEventReachesBusAndStompTopic() {
    Match match = manager.createMatch(ALICE, BOB, TC);
    Move legal = new ItalianRuleEngine().legalMoves(match.state()).get(0);

    manager.applyMove(match.id(), ALICE, legal);

    verify(bus, times(1)).publish(any());
    verify(stompPublisher, times(1))
        .publishToTopic(eq(MatchManager.MATCH_TOPIC_PREFIX + match.id()), any());
  }

  @Test
  void eventsSinceDelegatesToRepository() {
    Match match = manager.createMatch(ALICE, BOB, TC);
    Move legal = new ItalianRuleEngine().legalMoves(match.state()).get(0);
    manager.applyMove(match.id(), ALICE, legal);

    assertThat(manager.eventsSince(match.id(), -1L)).hasSize(1);
    assertThat(manager.eventsSince(match.id(), 0L)).isEmpty();
  }
}
