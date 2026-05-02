/**
 * Tournament domain: {@code Tournament} sealed interface ({@code EliminationTournament}, {@code
 * RoundRobinTournament}), the {@code TournamentEngine} service, and the stub generators {@code
 * BracketGenerator}, {@code RoundRobinScheduler}, {@code TieBreakerPolicy}.
 *
 * <p>Fase 4 ships only contracts plus a minimal {@code TournamentEngineImpl} that supports {@code
 * createTournament} and {@code registerParticipant} (status {@code CREATED}). The real logic —
 * {@code BracketGenerator.generate}, {@code RoundRobinScheduler.schedule}, {@code
 * TournamentEngine.startTournament} — throws {@code UnsupportedOperationException}; bracket
 * generation arrives in Fase 8 and round-robin scheduling/standings in Fase 9.
 */
package com.damaitaliana.core.tournament;
