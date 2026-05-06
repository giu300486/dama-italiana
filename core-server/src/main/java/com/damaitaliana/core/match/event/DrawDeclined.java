package com.damaitaliana.core.match.event;

import com.damaitaliana.core.match.MatchId;
import java.time.Instant;
import java.util.Objects;

/** The opponent declined a pending draw offer. The match continues. */
public record DrawDeclined(MatchId matchId, long sequenceNo, Instant timestamp)
    implements MatchEvent {

  public DrawDeclined {
    Objects.requireNonNull(matchId, "matchId");
    Objects.requireNonNull(timestamp, "timestamp");
    if (sequenceNo < 0) {
      throw new IllegalArgumentException("sequenceNo must be >= 0, got: " + sequenceNo);
    }
  }
}
