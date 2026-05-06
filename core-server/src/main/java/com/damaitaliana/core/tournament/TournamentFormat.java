package com.damaitaliana.core.tournament;

/**
 * Tournament format choice (SPEC §8.3 column {@code tournaments.format}). {@link
 * #SINGLE_ELIMINATION} yields an {@link EliminationTournament} (Fase 8); {@link #ROUND_ROBIN}
 * yields a {@link RoundRobinTournament} (Fase 9).
 */
public enum TournamentFormat {
  SINGLE_ELIMINATION,
  ROUND_ROBIN
}
