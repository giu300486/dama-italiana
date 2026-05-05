package com.damaitaliana.core.stomp;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link StompCompatiblePublisher}: logs the {@code (topic, payload)} pair via SLF4J at
 * {@code INFO} level. Used in Fase 4 standalone tests and in the {@code core-server}-only JVM runs
 * (no transport wired). The {@code MatchManager} of Task 4.8 will autowire whichever {@link
 * StompCompatiblePublisher} bean is marked {@code @Primary}; in Fase 6/7 the real {@code
 * WebSocketStompPublisher} takes that role and this logging fallback becomes orphaned (still
 * constructed but never invoked).
 *
 * <p><b>Deviation from PLAN-fase-4 §4.6</b>: the plan called for
 * {@code @ConditionalOnMissingBean(StompCompatiblePublisher.class)}, which lives in {@code
 * spring-boot-autoconfigure}. Pulling that artifact into {@code core-server} would break the
 * transport-agnostic invariant (CLAUDE.md §8.7-8.8: only {@code shared} + {@code spring-context}
 * allowed) and add a dependency outside SPEC §6 (anti-pattern #13). The plain {@code @Component} +
 * downstream {@code @Primary} pattern is the {@code spring-context}-only equivalent.
 */
@Component
public final class LoggingStompPublisher implements StompCompatiblePublisher {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingStompPublisher.class);

  @Override
  public void publishToTopic(String topic, Object payload) {
    Objects.requireNonNull(topic, "topic");
    Objects.requireNonNull(payload, "payload");
    LOGGER.info("[stomp] {} <- {}", topic, payload);
  }
}
