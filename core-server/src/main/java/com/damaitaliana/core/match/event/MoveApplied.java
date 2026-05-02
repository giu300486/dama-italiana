package com.damaitaliana.core.match.event;

import com.damaitaliana.core.match.MatchId;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import java.time.Instant;
import java.util.Objects;

/**
 * A legal move was applied to the match. The {@code newState} is the post-move {@link GameState}
 * (board, side-to-move flipped, halfmove clock updated, status possibly transitioned to a terminal
 * value).
 */
public record MoveApplied(
    MatchId matchId, long sequenceNo, Instant timestamp, Move move, GameState newState)
    implements MatchEvent {

  public MoveApplied {
    Objects.requireNonNull(matchId, "matchId");
    Objects.requireNonNull(timestamp, "timestamp");
    Objects.requireNonNull(move, "move");
    Objects.requireNonNull(newState, "newState");
    if (sequenceNo < 0) {
      throw new IllegalArgumentException("sequenceNo must be >= 0, got: " + sequenceNo);
    }
  }
}
