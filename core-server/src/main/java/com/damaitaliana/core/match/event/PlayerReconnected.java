package com.damaitaliana.core.match.event;

import com.damaitaliana.core.match.MatchId;
import com.damaitaliana.core.match.UserRef;
import java.time.Instant;
import java.util.Objects;

/** A previously disconnected player resumed their session. Replay of missed events follows. */
public record PlayerReconnected(MatchId matchId, long sequenceNo, Instant timestamp, UserRef who)
    implements MatchEvent {

  public PlayerReconnected {
    Objects.requireNonNull(matchId, "matchId");
    Objects.requireNonNull(timestamp, "timestamp");
    Objects.requireNonNull(who, "who");
    if (sequenceNo < 0) {
      throw new IllegalArgumentException("sequenceNo must be >= 0, got: " + sequenceNo);
    }
  }
}
