/**
 * In-memory adapters for the repository ports. Backed by {@link
 * java.util.concurrent.ConcurrentHashMap} for snapshot state, synchronized lists for the
 * append-only event log, and {@link java.util.concurrent.atomic.AtomicLong} for the monotonic
 * per-match sequence number (FR-COM-04, SPEC §7.5).
 *
 * <p>Used by the LAN host (the {@code client} module activates these via {@code
 * CoreServerConfiguration} when entering host mode in Fase 7) and by the unit/integration tests of
 * core-server itself. The Internet server (Fase 6) uses JPA adapters living in the {@code server}
 * module — those adapters re-implement the same ports against the {@code matches}/{@code
 * match_events} tables (SPEC §8.4).
 */
package com.damaitaliana.core.repository.inmemory;
