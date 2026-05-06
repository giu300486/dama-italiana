package com.damaitaliana.core.match.event;

import com.damaitaliana.core.match.MatchId;
import com.damaitaliana.core.match.RejectionReason;
import com.damaitaliana.core.match.UserRef;
import java.time.Instant;
import java.util.Objects;

/**
 * A move was refused by {@code MatchManager}. State is unchanged; the per-player anti-cheat counter
 * is incremented (5 consecutive rejections trigger a {@link MatchEnded} with reason {@code
 * FORFEIT_ANTI_CHEAT}, SPEC §9.8.3).
 *
 * <p>{@link RejectionReason} is the wire-stable code; localized human messages live in the
 * transport layer (Fase 6 server / Fase 7 client).
 */
public record MoveRejected(
    MatchId matchId, long sequenceNo, Instant timestamp, UserRef sender, RejectionReason reason)
    implements MatchEvent {

  public MoveRejected {
    Objects.requireNonNull(matchId, "matchId");
    Objects.requireNonNull(timestamp, "timestamp");
    Objects.requireNonNull(sender, "sender");
    Objects.requireNonNull(reason, "reason");
    if (sequenceNo < 0) {
      throw new IllegalArgumentException("sequenceNo must be >= 0, got: " + sequenceNo);
    }
  }
}
