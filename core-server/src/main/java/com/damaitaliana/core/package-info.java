/**
 * Transport-agnostic core: Tournament Engine, Match Manager, repository ports, in-memory adapters.
 *
 * <p>Sub-packages added in Fase 4+: {@code match}, {@code tournament}, {@code repository}, {@code
 * repository.inmemory}, {@code stomp}.
 *
 * <p>Constraint (CLAUDE.md §8.8): this module MUST NOT pull a transport (no Tomcat, no Jetty, no
 * JPA). Allowed dependencies: shared, spring-context (DI), spring-messaging (DTO only).
 */
package com.damaitaliana.core;
