package com.damaitaliana.core.match.event;

import com.damaitaliana.core.match.MatchId;
import com.damaitaliana.core.match.UserRef;
import java.time.Instant;
import java.util.Objects;

/**
 * A player resigned. The match transitions to {@code FINISHED} with the opponent as winner and
 * {@code reason=RESIGN}; a {@link MatchEnded} event follows.
 */
public record Resigned(MatchId matchId, long sequenceNo, Instant timestamp, UserRef who)
    implements MatchEvent {

  public Resigned {
    Objects.requireNonNull(matchId, "matchId");
    Objects.requireNonNull(timestamp, "timestamp");
    Objects.requireNonNull(who, "who");
    if (sequenceNo < 0) {
      throw new IllegalArgumentException("sequenceNo must be >= 0, got: " + sequenceNo);
    }
  }
}
