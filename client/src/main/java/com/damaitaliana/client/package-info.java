/**
 * Desktop client: JavaFX UI + Spring Boot DI (non-web) + on-demand embedded Jetty for LAN host.
 *
 * <p>Sub-packages: {@code app} (bootstrap and scene routing, Fase 3); {@code controller}, {@code
 * i18n}, {@code persistence}, {@code ui} (Fase 3); {@code network}, {@code lan.discovery}, {@code
 * lan.host} (Fase 6+).
 *
 * <p>Constraint (CLAUDE.md §8.6, ADR-005): when the WebSocket starter is in use, Tomcat MUST be
 * excluded and Jetty MUST be the embedded transport.
 */
package com.damaitaliana.client;
