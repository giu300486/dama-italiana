package com.damaitaliana.core.match.event;

import com.damaitaliana.core.match.EndReason;
import com.damaitaliana.core.match.MatchId;
import com.damaitaliana.core.match.MatchResult;
import java.time.Instant;
import java.util.Objects;

/**
 * Terminal event of a match. The match transitions to {@code FINISHED} (or {@code ABORTED} for
 * abnormal termination — Fase 4 does not emit the {@code ABORTED} variant; Fase 6 will when timeout
 * enforcement lands).
 *
 * <p>Carries both {@link MatchResult} (winner outcome) and {@link EndReason} (mechanism). The
 * dual-field design extends SPEC §8.3 (which fixes only {@code MatchResult}) with the granularity
 * the PLAN-fase-4 anti-cheat flow requires (5 illegal moves → {@code FORFEIT_ANTI_CHEAT}); the
 * choice is reviewable in REVIEW Fase 4.
 */
public record MatchEnded(
    MatchId matchId, long sequenceNo, Instant timestamp, MatchResult result, EndReason reason)
    implements MatchEvent {

  public MatchEnded {
    Objects.requireNonNull(matchId, "matchId");
    Objects.requireNonNull(timestamp, "timestamp");
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(reason, "reason");
    if (sequenceNo < 0) {
      throw new IllegalArgumentException("sequenceNo must be >= 0, got: " + sequenceNo);
    }
    if (result == MatchResult.UNFINISHED) {
      throw new IllegalArgumentException("MatchEnded requires a terminal result, got UNFINISHED");
    }
  }
}
