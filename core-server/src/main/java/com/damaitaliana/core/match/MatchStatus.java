package com.damaitaliana.core.match;

/**
 * Lifecycle state of a match (SPEC §8.4 column {@code matches.status}).
 *
 * <ul>
 *   <li>{@link #WAITING} — the match has been created but the second player has not joined yet.
 *       Fase 4 short-circuits this state when both players are known up-front; the full lobby flow
 *       arrives with Fase 6.
 *   <li>{@link #ONGOING} — both players are bound and moves are being applied.
 *   <li>{@link #FINISHED} — the match terminated normally (win, draw, resign, anti-cheat forfeit).
 *       A {@link com.damaitaliana.core.match.event.MatchEnded} event was emitted.
 *   <li>{@link #ABORTED} — the match terminated abnormally (both players disconnected past a
 *       timeout — Fase 6 enforces; Fase 4 leaves the enum value reserved).
 * </ul>
 */
public enum MatchStatus {
  WAITING,
  ONGOING,
  FINISHED,
  ABORTED
}
