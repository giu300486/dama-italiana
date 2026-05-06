/**
 * Match domain: aggregate {@code Match}, lifecycle service {@code MatchManager}, value types
 * ({@code MatchId}, {@code UserRef}, {@code TimeControl}, status enums).
 *
 * <p>The anti-cheat counter (SPEC §9.8.3 — 5 consecutive illegal moves trigger forfeit) lives here
 * as transient match state, mutated by {@code MatchManager} on every {@code applyMove}. The counter
 * is intentionally not persisted; it resets benevolently on recovery (rationale to be documented in
 * ADR-040, Task 4.13).
 *
 * <p>Constraint (CLAUDE.md §8.8): no transport (Tomcat, Jetty, JPA, JavaFX). Allowed deps: {@code
 * shared} (RuleEngine, Move, GameState), {@code spring-context} for DI.
 */
package com.damaitaliana.core.match;
