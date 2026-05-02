/**
 * Internal event bus over Spring's {@code ApplicationEventPublisher} (rationale to be documented in
 * ADR-038, Task 4.13). Publishes {@link com.damaitaliana.core.match.event.MatchEvent} wrapped in
 * {@code MatchEventEnvelope extends ApplicationEvent}.
 *
 * <p>Listeners register via {@code @EventListener(MatchEventEnvelope.class)} on any Spring bean.
 * FIFO order is preserved per publisher thread; cross-thread ordering depends on the publisher's
 * call site.
 *
 * <p>Constraint (CLAUDE.md §8.8): this bus is in-process only. Cross-JVM event distribution (e.g.
 * Redis pub-sub for horizontal scaling, SPEC §7.6) is out of scope for v1.
 */
package com.damaitaliana.core.eventbus;
