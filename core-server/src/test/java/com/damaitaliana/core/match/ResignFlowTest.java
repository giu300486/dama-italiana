package com.damaitaliana.core.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.damaitaliana.core.eventbus.MatchEventBus;
import com.damaitaliana.core.match.event.MatchEnded;
import com.damaitaliana.core.match.event.MatchEvent;
import com.damaitaliana.core.match.event.Resigned;
import com.damaitaliana.core.repository.inmemory.InMemoryMatchRepository;
import com.damaitaliana.core.stomp.StompCompatiblePublisher;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Resign flow tests for {@link MatchManager} (A4.7). */
@ExtendWith(MockitoExtension.class)
class ResignFlowTest {

  private static final Instant NOW = Instant.parse("2026-05-05T12:00:00Z");
  private static final UserRef ALICE = UserRef.anonymousLan("alice");
  private static final UserRef BOB = UserRef.anonymousLan("bob");
  private static final UserRef CAROL = UserRef.anonymousLan("carol");
  private static final TimeControl TC = TimeControl.unlimited();

  @Mock MatchEventBus bus;
  @Mock StompCompatiblePublisher stompPublisher;

  private MatchManager manager;

  @BeforeEach
  void setUp() {
    manager =
        new MatchManager(
            new InMemoryMatchRepository(),
            new ItalianRuleEngine(),
            bus,
            stompPublisher,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void resignByWhitePlayerEmitsResignedAndMatchEndedWithBlackAsWinner() {
    Match match = manager.createMatch(ALICE, BOB, TC);

    MatchEvent event = manager.resign(match.id(), ALICE);

    assertThat(event).isInstanceOf(Resigned.class);
    assertThat(((Resigned) event).who()).isEqualTo(ALICE);
    List<MatchEvent> events = manager.eventsSince(match.id(), -1L);
    assertThat(events).hasSize(2);
    assertThat(events.get(0)).isInstanceOf(Resigned.class);
    assertThat(events.get(1)).isInstanceOf(MatchEnded.class);
    MatchEnded ended = (MatchEnded) events.get(1);
    assertThat(ended.result()).isEqualTo(MatchResult.BLACK_WINS);
    assertThat(ended.reason()).isEqualTo(EndReason.RESIGN);
  }

  @Test
  void resignByBlackPlayerEmitsResignedAndMatchEndedWithWhiteAsWinner() {
    Match match = manager.createMatch(ALICE, BOB, TC);

    manager.resign(match.id(), BOB);

    MatchEnded ended = (MatchEnded) manager.eventsSince(match.id(), -1L).get(1);
    assertThat(ended.result()).isEqualTo(MatchResult.WHITE_WINS);
    assertThat(ended.reason()).isEqualTo(EndReason.RESIGN);
  }

  @Test
  void resignTransitionsMatchToFinished() {
    Match match = manager.createMatch(ALICE, BOB, TC);

    manager.resign(match.id(), ALICE);

    assertThat(match.status()).isEqualTo(MatchStatus.FINISHED);
  }

  @Test
  void resignOnAlreadyFinishedMatchThrows() {
    Match match = manager.createMatch(ALICE, BOB, TC);
    manager.resign(match.id(), ALICE);

    assertThatIllegalStateException()
        .isThrownBy(() -> manager.resign(match.id(), BOB))
        .withMessageContaining("non-ongoing");
  }

  @Test
  void resignByNonParticipantThrowsIllegalArgument() {
    Match match = manager.createMatch(ALICE, BOB, TC);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> manager.resign(match.id(), CAROL))
        .withMessageContaining("not a participant");
  }

  @Test
  void resignOnUnknownMatchThrowsMatchNotFound() {
    assertThatExceptionOfType(MatchNotFoundException.class)
        .isThrownBy(() -> manager.resign(MatchId.random(), ALICE));
  }
}
