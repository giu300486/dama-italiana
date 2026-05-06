/**
 * Repository ports (plain Java interfaces): {@code MatchRepository}, {@code TournamentRepository},
 * {@code UserRepository}.
 *
 * <p>Implementations are provided by adapters: {@link com.damaitaliana.core.repository.inmemory}
 * for the LAN host and core-server tests, JPA in the {@code server} module (Fase 6) outside
 * core-server.
 *
 * <p>Constraint (CLAUDE.md §8.8): ports MUST NOT depend on JPA, Hibernate, or any persistence
 * framework. They are plain Java interfaces — the {@code server} module wires them to JPA via its
 * own adapter classes.
 */
package com.damaitaliana.core.repository;
