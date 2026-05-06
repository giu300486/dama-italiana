package com.damaitaliana.core.match;

/**
 * Outcome of a match (SPEC §8.4 column {@code matches.result}).
 *
 * <p>While the match is in progress the persisted outcome is {@link #UNFINISHED}; on transition to
 * {@link MatchStatus#FINISHED} it becomes one of the three terminal values.
 */
public enum MatchResult {
  WHITE_WINS,
  BLACK_WINS,
  DRAW,
  UNFINISHED
}
