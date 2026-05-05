package com.damaitaliana.core.eventbus;

import com.damaitaliana.core.match.event.MatchEvent;

/**
 * In-process publisher of {@link MatchEvent} occurrences. The default Fase 4 implementation ({@link
 * SpringMatchEventBus}) delegates to Spring's {@link
 * org.springframework.context.ApplicationEventPublisher}; listeners register via
 * {@code @EventListener void on(MatchEventEnvelope env)} on any bean managed by the {@link
 * com.damaitaliana.core.CoreServerConfiguration} context.
 *
 * <p>Order semantics: events published from a single thread are delivered to a single listener in
 * the same order (FIFO per publisher thread, A4.9). Spring's default event multicaster is
 * synchronous — the call to {@link #publish(MatchEvent)} returns only after all listeners have
 * processed the event. The {@code MatchManager} of Task 4.8 is responsible for serializing writes
 * per match, so per-match FIFO follows from per-thread FIFO.
 *
 * <p>Constraint (CLAUDE.md §8.8): in-process only. Cross-JVM event distribution rides the STOMP
 * publisher (Task 4.6, real impl in Fase 6/7), not this bus.
 */
public interface MatchEventBus {

  /**
   * Publishes {@code event} to all registered {@link MatchEventEnvelope} listeners. Wrapping
   * preserves listener filtering on a single Spring event type instead of forcing each listener to
   * subscribe to all nine sealed permits of {@link MatchEvent}.
   */
  void publish(MatchEvent event);
}
