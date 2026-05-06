package com.damaitaliana.core.match;

/**
 * Mechanism that ended a match — carried by {@link com.damaitaliana.core.match.event.MatchEnded}
 * alongside {@link MatchResult}.
 *
 * <p>Note: this granularity goes beyond what SPEC §8.3 strictly requires (which only fixes {@code
 * MatchResult}) but matches the PLAN-fase-4 contract for the anti-cheat forfeit flow (5 illegal
 * moves → {@code FORFEIT_ANTI_CHEAT}, SPEC §9.8.3). Reviewable in REVIEW Fase 4 if deemed surplus.
 */
public enum EndReason {
  /** The last legal move left the opponent without legal moves (or no pieces). */
  CHECKMATE_LIKE,

  /** A player resigned via {@code MatchManager.resign}. */
  RESIGN,

  /** Both players accepted a draw offer. */
  DRAW_AGREEMENT,

  /** Threefold repetition (SPEC §3.6). */
  DRAW_REPETITION,

  /** 40-move rule (SPEC §3.6). */
  DRAW_FORTY_MOVES,

  /** A player accumulated 5 consecutive illegal moves (SPEC §9.8.3). */
  FORFEIT_ANTI_CHEAT,

  /** Time control expired for one player (Fase 6 enforces; Fase 4 reserves the value). */
  FORFEIT_TIMEOUT
}
