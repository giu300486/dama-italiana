package com.damaitaliana.core.match.event;

import com.damaitaliana.core.match.MatchId;
import com.damaitaliana.core.match.UserRef;
import java.time.Instant;
import java.util.Objects;

/** A player offered a draw via {@code MatchManager.offerDraw}. */
public record DrawOffered(MatchId matchId, long sequenceNo, Instant timestamp, UserRef from)
    implements MatchEvent {

  public DrawOffered {
    Objects.requireNonNull(matchId, "matchId");
    Objects.requireNonNull(timestamp, "timestamp");
    Objects.requireNonNull(from, "from");
    if (sequenceNo < 0) {
      throw new IllegalArgumentException("sequenceNo must be >= 0, got: " + sequenceNo);
    }
  }
}
