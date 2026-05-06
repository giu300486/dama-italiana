package com.damaitaliana.core.stomp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Test-scope {@link StompCompatiblePublisher} that buffers each {@code (topic, payload)} pair so
 * unit tests can assert that events reached the expected topic in the expected order (A4.10).
 *
 * <p>Lives in {@code core-server/src/test/java} so it is not packaged into the production jar. The
 * Fase 4 {@code MatchManager} tests of Task 4.8 inject this bean instead of {@link
 * LoggingStompPublisher} via a {@code @TestConfiguration} or by wiring it manually with {@link
 * org.springframework.context.annotation.AnnotationConfigApplicationContext#register}.
 *
 * <p>Concurrency: {@link Collections#synchronizedList(List) synchronized list} for the buffer;
 * {@link #published()} takes the snapshot under the list's intrinsic lock so iteration is safe
 * under concurrent {@link #publishToTopic} calls.
 */
public final class BufferingStompPublisher implements StompCompatiblePublisher {

  /**
   * One captured publication. The {@code payload} is referenced as {@link Object} so any {@code
   * MatchEvent} (or other arbitrary type) can be buffered without losing identity.
   */
  public record Published(String topic, Object payload) {

    public Published {
      Objects.requireNonNull(topic, "topic");
      Objects.requireNonNull(payload, "payload");
    }
  }

  private final List<Published> buffer = Collections.synchronizedList(new ArrayList<>());

  @Override
  public void publishToTopic(String topic, Object payload) {
    buffer.add(new Published(topic, payload));
  }

  /** Returns an immutable snapshot of buffered publications, in publish order. */
  public List<Published> published() {
    synchronized (buffer) {
      return List.copyOf(buffer);
    }
  }

  /** Empties the buffer — used between scenarios in test fixtures. */
  public void clear() {
    buffer.clear();
  }
}
