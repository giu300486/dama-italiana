package com.damaitaliana.core.tournament;

/** Lifecycle state of a tournament (SPEC §8.4 column {@code tournaments.status}). */
public enum TournamentStatus {
  CREATED,
  IN_PROGRESS,
  FINISHED
}
