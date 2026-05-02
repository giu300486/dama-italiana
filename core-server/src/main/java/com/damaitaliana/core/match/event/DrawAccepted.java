package com.damaitaliana.core.match.event;

import com.damaitaliana.core.match.MatchId;
import java.time.Instant;
import java.util.Objects;

/**
 * The opponent accepted a pending draw offer. The match transitions to {@code FINISHED} with {@code
 * result=DRAW} and {@code reason=DRAW_AGREEMENT}; a {@link MatchEnded} event follows.
 */
public record DrawAccepted(MatchId matchId, long sequenceNo, Instant timestamp)
    implements MatchEvent {

  public DrawAccepted {
    Objects.requireNonNull(matchId, "matchId");
    Objects.requireNonNull(timestamp, "timestamp");
    if (sequenceNo < 0) {
      throw new IllegalArgumentException("sequenceNo must be >= 0, got: " + sequenceNo);
    }
  }
}
