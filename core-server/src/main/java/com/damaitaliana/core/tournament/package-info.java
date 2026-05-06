/**
 * Tournament domain: {@code Tournament} sealed interface ({@code EliminationTournament}, {@code
 * RoundRobinTournament}), the {@code TournamentEngine} service with input record {@code
 * TournamentSpec} and {@code TournamentFormat} enum, and the stub generators {@code
 * BracketGenerator} / {@code RoundRobinScheduler} / {@code TieBreakerPolicy}.
 *
 * <p>Fase 4 ships only contracts plus a minimal {@link TournamentEngineImpl} that supports {@code
 * createTournament} and {@code registerParticipant} (status {@code CREATED}). The real logic —
 * {@link BracketGenerator#generate}, {@link RoundRobinScheduler#schedule}, {@link
 * TournamentEngine#startTournament} — throws {@code UnsupportedOperationException} via the {@link
 * StubBracketGenerator}/{@link StubRoundRobinScheduler} placeholders and the engine's own stub.
 * Bracket generation arrives in Fase 8 and round-robin scheduling/standings in Fase 9.
 */
package com.damaitaliana.core.tournament;
