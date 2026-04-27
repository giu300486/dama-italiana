/**
 * Desktop client: JavaFX UI + Spring Boot DI (non-web) + on-demand embedded Jetty for LAN host.
 *
 * <p>Sub-packages added in Fase 3+: {@code ui}, {@code controller}, {@code network}, {@code
 * lan.discovery}, {@code lan.host}, {@code persistence}.
 *
 * <p>Constraint (CLAUDE.md §8.6, ADR-005): when the WebSocket starter is in use, Tomcat MUST be
 * excluded and Jetty MUST be the embedded transport.
 */
package com.damaitaliana.client;
