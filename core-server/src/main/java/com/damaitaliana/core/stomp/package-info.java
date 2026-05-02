/**
 * STOMP-compatible publisher port (rationale to be documented in ADR-039, Task 4.13). The interface
 * contract is "given a topic and a payload, propagate it to the transport layer" — core-server
 * itself does not know about WebSocket, Tomcat, or Jetty.
 *
 * <p>Fase 4 ships {@code LoggingStompPublisher} (SLF4J only, default fallback bean via
 * {@code @ConditionalOnMissingBean}) and {@code BufferingStompPublisher} (test-scope, in {@code
 * core-server/src/test/java}, used by unit tests to assert that events reach the expected topic).
 * Real WebSocket implementations are wired by {@code server} (Fase 6, Internet) and {@code client}
 * LAN host (Fase 7).
 */
package com.damaitaliana.core.stomp;
