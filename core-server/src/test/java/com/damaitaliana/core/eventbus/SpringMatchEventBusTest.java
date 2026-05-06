package com.damaitaliana.core.eventbus;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.core.CoreServerConfiguration;
import com.damaitaliana.core.match.MatchId;
import com.damaitaliana.core.match.UserRef;
import com.damaitaliana.core.match.event.DrawAccepted;
import com.damaitaliana.core.match.event.DrawDeclined;
import com.damaitaliana.core.match.event.DrawOffered;
import com.damaitaliana.core.match.event.MatchEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Integration test for the Spring-backed event bus (A4.9): bootstraps the {@link
 * CoreServerConfiguration} context plus a {@link CollectingListener} bean, publishes a sequence of
 * events through {@link MatchEventBus#publish}, and asserts that the listener received them in
 * publish order.
 *
 * <p><b>Deviation from PLAN-fase-4 §4.5</b>: the plan called for {@code @SpringJUnitConfig}, which
 * lives in the {@code spring-test} artifact. {@code spring-test} is not currently a {@code
 * core-server} dependency and adding it would introduce a new dep without explicit approval
 * (CLAUDE.md §8 anti-pattern #13). The test instead bootstraps {@link
 * AnnotationConfigApplicationContext} directly — same pattern as {@code
 * CoreServerConfigurationTest} — which exercises the same Spring event infrastructure without
 * expanding the dep set.
 */
class SpringMatchEventBusTest {

  private static final Instant NOW = Instant.parse("2026-05-05T12:00:00Z");
  private static final UserRef ALICE = UserRef.anonymousLan("alice");
  private static final UserRef BOB = UserRef.anonymousLan("bob");

  /**
   * Test bean — collects every {@link MatchEventEnvelope} delivered by the bus. {@code
   * synchronizedList} guards against accidental concurrent publication if the test ever evolves to
   * multi-threaded scenarios.
   */
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

  @Test
  void eventListenerReceivesAllEventsInPublishOrder() {
    try (AnnotationConfigApplicationContext context = newContext()) {
      MatchEventBus bus = context.getBean(MatchEventBus.class);
      CollectingListener listener = context.getBean(CollectingListener.class);

      MatchId mid = MatchId.random();
      DrawOffered e0 = new DrawOffered(mid, 0L, NOW, ALICE);
      DrawDeclined e1 = new DrawDeclined(mid, 1L, NOW);
      DrawOffered e2 = new DrawOffered(mid, 2L, NOW, BOB);
      DrawAccepted e3 = new DrawAccepted(mid, 3L, NOW);

      bus.publish(e0);
      bus.publish(e1);
      bus.publish(e2);
      bus.publish(e3);

      assertThat(listener.received()).containsExactly(e0, e1, e2, e3);
    }
  }

  @Test
  void publishingFromSingleThreadDeliversFifoPerMatch() {
    try (AnnotationConfigApplicationContext context = newContext()) {
      MatchEventBus bus = context.getBean(MatchEventBus.class);
      CollectingListener listener = context.getBean(CollectingListener.class);

      MatchId mA = MatchId.random();
      MatchId mB = MatchId.random();
      DrawOffered a0 = new DrawOffered(mA, 0L, NOW, ALICE);
      DrawOffered b0 = new DrawOffered(mB, 0L, NOW, BOB);
      DrawDeclined a1 = new DrawDeclined(mA, 1L, NOW);
      DrawDeclined b1 = new DrawDeclined(mB, 1L, NOW);

      bus.publish(a0);
      bus.publish(b0);
      bus.publish(a1);
      bus.publish(b1);

      List<MatchEvent> matchA =
          listener.received().stream().filter(e -> e.matchId().equals(mA)).toList();
      List<MatchEvent> matchB =
          listener.received().stream().filter(e -> e.matchId().equals(mB)).toList();
      assertThat(matchA).containsExactly(a0, a1);
      assertThat(matchB).containsExactly(b0, b1);
    }
  }

  @Test
  void busBeanIsTheSpringImplementation() {
    try (AnnotationConfigApplicationContext context = newContext()) {
      assertThat(context.getBean(MatchEventBus.class)).isInstanceOf(SpringMatchEventBus.class);
    }
  }

  @Test
  void publishingNullEventThrowsNullPointerException() {
    try (AnnotationConfigApplicationContext context = newContext()) {
      MatchEventBus bus = context.getBean(MatchEventBus.class);
      org.assertj.core.api.Assertions.assertThatNullPointerException()
          .isThrownBy(() -> bus.publish(null));
    }
  }

  private static AnnotationConfigApplicationContext newContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(CoreServerConfiguration.class);
    context.register(CollectingListener.class);
    context.refresh();
    return context;
  }
}
