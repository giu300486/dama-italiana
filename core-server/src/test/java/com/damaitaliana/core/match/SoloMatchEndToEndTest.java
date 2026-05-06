package com.damaitaliana.core.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.core.CoreServerConfiguration;
import com.damaitaliana.core.eventbus.MatchEventBus;
import com.damaitaliana.core.eventbus.MatchEventEnvelope;
import com.damaitaliana.core.match.event.MatchEnded;
import com.damaitaliana.core.match.event.MatchEvent;
import com.damaitaliana.core.match.event.MoveApplied;
import com.damaitaliana.core.repository.inmemory.InMemoryMatchRepository;
import com.damaitaliana.core.stomp.BufferingStompPublisher;
import com.damaitaliana.core.stomp.StompCompatiblePublisher;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * End-to-end Java API test (PLAN-fase-4 §4.11). Drives {@link MatchManager} through a full random
 * game using the real {@link ItalianRuleEngine}, then asserts the three core acceptance properties
 * of Fase 4:
 *
 * <ol>
 *   <li>{@link #playsRandomGameUntilTerminal} — the loop terminates with status FINISHED, the event
 *       log ends with a {@link MatchEnded}, and counts add up.
 *   <li>{@link #replayFromEventsReconstructsCurrentState} — folding {@link MoveApplied} events from
 *       {@link GameState#initial()} via the engine reproduces the live {@code match.state()}.
 *   <li>{@link #eventsBroadcastOnBusAndStomp} — the same event sequence reaches both the internal
 *       {@link MatchEventBus} (via a {@code @EventListener}) and the {@link
 *       StompCompatiblePublisher} (via {@link BufferingStompPublisher}), in identical order.
 * </ol>
 *
 * <p>Determinism: {@link Random} seeded with {@link #RANDOM_SEED} so the chosen move at each ply is
 * stable across runs. The safety cap {@link #MAX_PLIES} guards against the seed picking a path that
 * never terminates (theoretically impossible in Italian draughts — stalemate is a loss — but the
 * cap is a defensive ceiling and yields a clearer failure than an infinite loop).
 */
class SoloMatchEndToEndTest {

  private static final long RANDOM_SEED = 42L;
  private static final int MAX_PLIES = 500;

  private static final Instant NOW = Instant.parse("2026-05-05T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final UserRef ALICE = UserRef.anonymousLan("alice");
  private static final UserRef BOB = UserRef.anonymousLan("bob");
  private static final TimeControl TC = TimeControl.unlimited();

  // ---------------------------------------------------------------------------
  // (1) playsRandomGameUntilTerminal
  // ---------------------------------------------------------------------------

  @Test
  void playsRandomGameUntilTerminal() {
    InMemoryMatchRepository repo = new InMemoryMatchRepository();
    RuleEngine engine = new ItalianRuleEngine();
    MatchEventBus bus = Mockito.mock(MatchEventBus.class);
    StompCompatiblePublisher stomp = Mockito.mock(StompCompatiblePublisher.class);
    MatchManager manager = new MatchManager(repo, engine, bus, stomp, CLOCK);

    Match match = manager.createMatch(ALICE, BOB, TC);

    int pliesPlayed = playRandomGame(manager, engine, match);

    assertThat(match.status()).isEqualTo(MatchStatus.FINISHED);

    List<MatchEvent> events = manager.eventsSince(match.id(), -1L);
    // Each ply produces exactly one MoveApplied (no rejections in random play that picks from the
    // engine's own legal-move list); a single MatchEnded follows the terminal move.
    assertThat(events).hasSize(pliesPlayed + 1);
    assertThat(events).allMatch(e -> e instanceof MoveApplied || e instanceof MatchEnded);
    assertThat(events.get(events.size() - 1)).isInstanceOf(MatchEnded.class);
    // Sequence numbers strictly monotonic from 0.
    for (int i = 0; i < events.size(); i++) {
      assertThat(events.get(i).sequenceNo()).isEqualTo((long) i);
    }
  }

  // ---------------------------------------------------------------------------
  // (2) replayFromEventsReconstructsCurrentState
  // ---------------------------------------------------------------------------

  @Test
  void replayFromEventsReconstructsCurrentState() {
    InMemoryMatchRepository repo = new InMemoryMatchRepository();
    RuleEngine engine = new ItalianRuleEngine();
    MatchEventBus bus = Mockito.mock(MatchEventBus.class);
    StompCompatiblePublisher stomp = Mockito.mock(StompCompatiblePublisher.class);
    MatchManager manager = new MatchManager(repo, engine, bus, stomp, CLOCK);

    Match match = manager.createMatch(ALICE, BOB, TC);
    playFixedNumberOfPlies(manager, engine, match, 8);

    GameState replayed = replayFromInitial(engine, manager.eventsSince(match.id(), -1L));

    assertThat(replayed).isEqualTo(match.state());
  }

  // ---------------------------------------------------------------------------
  // (3) eventsBroadcastOnBusAndStomp
  // ---------------------------------------------------------------------------

  @Test
  void eventsBroadcastOnBusAndStomp() {
    try (AnnotationConfigApplicationContext context = newSpringContext()) {
      MatchManager manager = context.getBean(MatchManager.class);
      RuleEngine engine = context.getBean(RuleEngine.class);
      CollectingListener listener = context.getBean(CollectingListener.class);
      BufferingStompPublisher publisher = context.getBean(BufferingStompPublisher.class);

      Match match = manager.createMatch(ALICE, BOB, TC);
      playRandomGame(manager, engine, match);

      List<MatchEvent> busEvents = listener.received();
      List<MatchEvent> stompEvents =
          publisher.published().stream().map(p -> (MatchEvent) p.payload()).toList();

      assertThat(busEvents).isNotEmpty();
      assertThat(stompEvents).containsExactlyElementsOf(busEvents);
      // Every STOMP publish targets the per-match topic.
      assertThat(publisher.published())
          .allSatisfy(p -> assertThat(p.topic()).isEqualTo("/topic/match/" + match.id()));
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /**
   * Loops random-pick legal moves until the engine reports a terminal status, returning the number
   * of plies applied. Throws if {@link #MAX_PLIES} is reached — a defensive ceiling that should
   * never trigger for the Italian engine on the chosen seed.
   */
  private static int playRandomGame(MatchManager manager, RuleEngine engine, Match match) {
    Random rng = new Random(RANDOM_SEED);
    int plies = 0;
    while (match.status() == MatchStatus.ONGOING) {
      if (plies >= MAX_PLIES) {
        throw new AssertionError(
            "Random game did not terminate within "
                + MAX_PLIES
                + " plies (seed "
                + RANDOM_SEED
                + "); pick a different seed or raise the cap");
      }
      List<Move> legal = engine.legalMoves(match.state());
      Move pick = legal.get(rng.nextInt(legal.size()));
      UserRef sender =
          match.state().sideToMove() == com.damaitaliana.shared.domain.Color.WHITE ? ALICE : BOB;
      manager.applyMove(match.id(), sender, pick);
      plies++;
    }
    return plies;
  }

  private static void playFixedNumberOfPlies(
      MatchManager manager, RuleEngine engine, Match match, int plies) {
    Random rng = new Random(RANDOM_SEED);
    for (int i = 0; i < plies; i++) {
      List<Move> legal = engine.legalMoves(match.state());
      Move pick = legal.get(rng.nextInt(legal.size()));
      UserRef sender =
          match.state().sideToMove() == com.damaitaliana.shared.domain.Color.WHITE ? ALICE : BOB;
      manager.applyMove(match.id(), sender, pick);
    }
  }

  private static GameState replayFromInitial(RuleEngine engine, List<MatchEvent> events) {
    GameState state = GameState.initial();
    for (MatchEvent e : events) {
      if (e instanceof MoveApplied applied) {
        state = engine.applyMove(state, applied.move());
      }
    }
    return state;
  }

  private static AnnotationConfigApplicationContext newSpringContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(CoreServerConfiguration.class);
    context.register(BroadcastTestConfig.class);
    context.register(CollectingListener.class);
    context.refresh();
    return context;
  }

  /**
   * Test {@code @Configuration} that overrides {@link
   * com.damaitaliana.core.stomp.LoggingStompPublisher} with a {@link BufferingStompPublisher}
   * marked {@code @Primary} so the autowire on {@link MatchManager} resolves to it. Same approach
   * Fase 6 uses to swap the production {@code WebSocketStompPublisher} into core-server tests.
   */
  @Configuration
  static class BroadcastTestConfig {

    @Bean
    @Primary
    BufferingStompPublisher bufferingStompPublisher() {
      return new BufferingStompPublisher();
    }
  }

  /** Spring bean that collects every {@link MatchEvent} delivered through the internal bus. */
  @Component
  static final class CollectingListener {

    private final List<MatchEvent> received = Collections.synchronizedList(new ArrayList<>());

    @EventListener
    void on(MatchEventEnvelope envelope) {
      received.add(envelope.payload());
    }

    List<MatchEvent> received() {
      return List.copyOf(received);
    }
  }
}
