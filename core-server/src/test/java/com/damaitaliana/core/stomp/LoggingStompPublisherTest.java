package com.damaitaliana.core.stomp;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

/**
 * Smoke test for {@link LoggingStompPublisher}: the publish path is exercised so the SLF4J call is
 * covered by JaCoCo, and the null-argument validation is asserted explicitly. Log output is not
 * asserted — capturing SLF4J output would require adding a logging-test dependency outside SPEC §6
 * (CLAUDE.md §8 anti-pattern #13).
 */
class LoggingStompPublisherTest {

  private final LoggingStompPublisher publisher = new LoggingStompPublisher();

  @Test
  void publishToTopicAcceptsAnyTopicAndPayloadWithoutThrowing() {
    publisher.publishToTopic("/topic/match/abc", "hello");
    publisher.publishToTopic("/topic/match/xyz", new StringBuilder("payload-with-toString"));
    publisher.publishToTopic("/topic/lobby", 42);
  }

  @Test
  void publishToTopicRejectsNullTopic() {
    assertThatNullPointerException()
        .isThrownBy(() -> publisher.publishToTopic(null, "payload"))
        .withMessageContaining("topic");
  }

  @Test
  void publishToTopicRejectsNullPayload() {
    assertThatNullPointerException()
        .isThrownBy(() -> publisher.publishToTopic("/topic/match/abc", null))
        .withMessageContaining("payload");
  }
}
