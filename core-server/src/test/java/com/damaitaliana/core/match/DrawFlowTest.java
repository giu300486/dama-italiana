package com.damaitaliana.core.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.damaitaliana.core.eventbus.MatchEventBus;
import com.damaitaliana.core.match.event.DrawAccepted;
import com.damaitaliana.core.match.event.DrawDeclined;
import com.damaitaliana.core.match.event.DrawOffered;
import com.damaitaliana.core.match.event.MatchEnded;
import com.damaitaliana.core.match.event.MatchEvent;
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

/** Draw offer/response flow tests for {@link MatchManager} (A4.8 — 4 scenarios). */
@ExtendWith(MockitoExtension.class)
class DrawFlowTest {

  private static final Instant NOW = Instant.parse("2026-05-05T12:00:00Z");
  private static final UserRef ALICE = UserRef.anonymousLan("alice");
  private static final UserRef BOB = UserRef.anonymousLan("bob");
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
  void offerDrawByWhiteEmitsDrawOfferedAndSetsPendingState() {
    Match match = manager.createMatch(ALICE, BOB, TC);

    MatchEvent event = manager.offerDraw(match.id(), ALICE);

    assertThat(event).isInstanceOf(DrawOffered.class);
    assertThat(((DrawOffered) event).from()).isEqualTo(ALICE);
    assertThat(match.pendingDrawOfferFrom()).contains(ALICE);
    assertThat(match.status()).isEqualTo(MatchStatus.ONGOING);
  }

  @Test
  void respondAcceptByOpponentEmitsDrawAcceptedAndMatchEndedWithDrawAgreement() {
    Match match = manager.createMatch(ALICE, BOB, TC);
    manager.offerDraw(match.id(), ALICE);

    MatchEvent event = manager.respondDraw(match.id(), BOB, true);

    assertThat(event).isInstanceOf(DrawAccepted.class);
    assertThat(match.status()).isEqualTo(MatchStatus.FINISHED);
    assertThat(match.pendingDrawOfferFrom()).isEmpty();
    List<MatchEvent> events = manager.eventsSince(match.id(), -1L);
    assertThat(events).hasSize(3);
    assertThat(events.get(0)).isInstanceOf(DrawOffered.class);
    assertThat(events.get(1)).isInstanceOf(DrawAccepted.class);
    assertThat(events.get(2)).isInstanceOf(MatchEnded.class);
    MatchEnded ended = (MatchEnded) events.get(2);
    assertThat(ended.result()).isEqualTo(MatchResult.DRAW);
    assertThat(ended.reason()).isEqualTo(EndReason.DRAW_AGREEMENT);
  }

  @Test
  void respondDeclineByOpponentEmitsDrawDeclinedAndMatchContinuesOngoing() {
    Match match = manager.createMatch(ALICE, BOB, TC);
    manager.offerDraw(match.id(), ALICE);

    MatchEvent event = manager.respondDraw(match.id(), BOB, false);

    assertThat(event).isInstanceOf(DrawDeclined.class);
    assertThat(match.status()).isEqualTo(MatchStatus.ONGOING);
    assertThat(match.pendingDrawOfferFrom()).isEmpty();
    // Match continues — players can offer again later.
    manager.offerDraw(match.id(), ALICE); // does not throw
  }

  @Test
  void secondOfferWhileFirstStillPendingThrows() {
    Match match = manager.createMatch(ALICE, BOB, TC);
    manager.offerDraw(match.id(), ALICE);

    assertThatIllegalStateException()
        .isThrownBy(() -> manager.offerDraw(match.id(), BOB))
        .withMessageContaining("draw offer is already pending");
  }

  @Test
  void respondingToOwnOfferThrows() {
    Match match = manager.createMatch(ALICE, BOB, TC);
    manager.offerDraw(match.id(), ALICE);

    assertThatIllegalStateException()
        .isThrownBy(() -> manager.respondDraw(match.id(), ALICE, true))
        .withMessageContaining("cannot respond to their own draw offer");
  }

  @Test
  void respondingWhenNoOfferIsPendingThrows() {
    Match match = manager.createMatch(ALICE, BOB, TC);

    assertThatIllegalStateException()
        .isThrownBy(() -> manager.respondDraw(match.id(), BOB, true))
        .withMessageContaining("No pending draw offer");
  }
}
