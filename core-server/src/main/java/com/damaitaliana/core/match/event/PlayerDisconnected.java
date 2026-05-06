package com.damaitaliana.core.match.event;

import com.damaitaliana.core.match.MatchId;
import com.damaitaliana.core.match.UserRef;
import java.time.Instant;
import java.util.Objects;

/**
 * A player's transport session dropped. The match is not auto-terminated; the player has a grace
 * period (Fase 6 enforces) to reconnect via {@link PlayerReconnected}.
 */
public record PlayerDisconnected(MatchId matchId, long sequenceNo, Instant timestamp, UserRef who)
    implements MatchEvent {

  public PlayerDisconnected {
    Objects.requireNonNull(matchId, "matchId");
    Objects.requireNonNull(timestamp, "timestamp");
    Objects.requireNonNull(who, "who");
    if (sequenceNo < 0) {
      throw new IllegalArgumentException("sequenceNo must be >= 0, got: " + sequenceNo);
    }
  }
}
