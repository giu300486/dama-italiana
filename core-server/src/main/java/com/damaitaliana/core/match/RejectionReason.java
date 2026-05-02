package com.damaitaliana.core.match;

/**
 * Reason carried by {@link com.damaitaliana.core.match.event.MoveRejected} when {@code
 * MatchManager} refuses to apply a move.
 *
 * <p>Localization of the human-facing message is the transport layer's responsibility (Fase 6
 * server / Fase 7 client) — the enum value is the wire-stable code.
 */
public enum RejectionReason {
  NOT_YOUR_TURN,
  ILLEGAL_MOVE,
  MATCH_NOT_ONGOING,
  MATCH_NOT_FOUND
}
