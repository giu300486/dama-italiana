package com.damaitaliana.core.eventbus;

import com.damaitaliana.core.match.event.MatchEvent;
import java.util.Objects;
import org.springframework.context.ApplicationEvent;

/**
 * Spring envelope wrapping a {@link MatchEvent} so it can travel through the {@link
 * org.springframework.context.ApplicationEventPublisher} infrastructure. The {@code source} (set by
 * {@link ApplicationEvent}) carries the publisher (typically the {@link SpringMatchEventBus} bean);
 * {@link #payload()} returns the wrapped match event for pattern-matching in listeners.
 *
 * <p>Listeners register a single signature:
 *
 * <pre>{@code
 * @EventListener
 * void on(MatchEventEnvelope envelope) {
 *   switch (envelope.payload()) {
 *     case MoveApplied a -> ...
 *     case MatchEnded e -> ...
 *     // ... exhaustive on the sealed MatchEvent
 *   }
 * }
 * }</pre>
 *
 * <p>{@link ApplicationEvent} implements {@link java.io.Serializable}, but instances of this
 * envelope are not intended to be serialized — the bus is in-process. The {@code transient} marker
 * on {@link #payload} reflects that intent and silences SpotBugs SE_BAD_FIELD warnings (the wire
 * layer of Fase 6/7 serializes the {@link MatchEvent} payload itself via Jackson, not the Spring
 * envelope).
 */
public final class MatchEventEnvelope extends ApplicationEvent {

  private static final long serialVersionUID = 1L;

  private final transient MatchEvent payload;

  public MatchEventEnvelope(Object source, MatchEvent payload) {
    super(source);
    this.payload = Objects.requireNonNull(payload, "payload");
  }

  /** The wrapped match event. */
  public MatchEvent payload() {
    return payload;
  }
}
