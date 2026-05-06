package com.damaitaliana.core.eventbus;

import com.damaitaliana.core.match.event.MatchEvent;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Default {@link MatchEventBus} implementation: wraps each event in a {@link MatchEventEnvelope}
 * and forwards it through Spring's {@link ApplicationEventPublisher}. Auto-discovered by {@link
 * com.damaitaliana.core.CoreServerConfiguration}'s component scan; injected with the application
 * context (which implements {@code ApplicationEventPublisher}).
 *
 * <p>Listeners across the {@code core-server} bound context register via {@code @EventListener void
 * on(MatchEventEnvelope env)} and pattern-match on {@code env.payload()} — the sealed {@link
 * MatchEvent} ensures exhaustiveness at compile time.
 */
@Component
public final class SpringMatchEventBus implements MatchEventBus {

  private final ApplicationEventPublisher publisher;

  public SpringMatchEventBus(ApplicationEventPublisher publisher) {
    this.publisher = Objects.requireNonNull(publisher, "publisher");
  }

  @Override
  public void publish(MatchEvent event) {
    Objects.requireNonNull(event, "event");
    publisher.publishEvent(new MatchEventEnvelope(this, event));
  }
}
