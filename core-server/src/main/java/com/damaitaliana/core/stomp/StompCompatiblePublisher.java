package com.damaitaliana.core.stomp;

/**
 * Port for transport-side broadcast. Implementations propagate {@code payload} to subscribers of
 * {@code topic} on the underlying STOMP/WebSocket layer (SPEC §11.4 {@code /topic/match/{id}}).
 * {@code core-server} itself knows nothing about WebSocket: the contract is "given a topic and a
 * payload, deliver the payload to whoever is subscribed to that topic on the transport".
 *
 * <p>Fase 4 ships two implementations:
 *
 * <ul>
 *   <li>{@link LoggingStompPublisher} (production, SLF4J only) — the default fallback when no
 *       transport is wired. Used by {@code core-server} unit tests and by the standalone {@code
 *       server}/{@code client} JVMs before they install their real publisher.
 *   <li>{@code BufferingStompPublisher} (test-scope, {@code core-server/src/test/java}) — collects
 *       {@code (topic, payload)} pairs for assertions in unit tests.
 * </ul>
 *
 * <p>Fase 6 ({@code server}) and Fase 7 ({@code client} LAN host) provide their own {@code
 * WebSocketStompPublisher @Component @Primary} bean that wraps Spring's {@code
 * SimpMessagingTemplate}. The {@code @Primary} marker makes the WebSocket impl the one autowired
 * into {@code MatchManager} when both beans coexist; the logging publisher remains registered but
 * orphaned (Spring's container does not multicast through unrelated beans of the same type — only
 * the autowired target receives calls from {@code MatchManager}).
 */
public interface StompCompatiblePublisher {

  /**
   * Sends {@code payload} to subscribers of {@code topic}. Implementations MUST NOT throw on
   * transport failure — they should log and swallow, since the broadcast is best-effort relative to
   * the authoritative server-side state already persisted via {@code MatchRepository}.
   *
   * @throws NullPointerException if either argument is {@code null} (validated up front so mistakes
   *     surface fast in unit tests rather than as transport-layer NPEs).
   */
  void publishToTopic(String topic, Object payload);
}
