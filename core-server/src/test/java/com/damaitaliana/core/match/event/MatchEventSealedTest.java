package com.damaitaliana.core.match.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.core.match.EndReason;
import com.damaitaliana.core.match.MatchId;
import com.damaitaliana.core.match.MatchResult;
import com.damaitaliana.core.match.RejectionReason;
import com.damaitaliana.core.match.UserRef;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies that pattern matching on {@link MatchEvent} is exhaustive — adding a new variant to the
 * sealed interface forces the {@code switch} below to break compilation, preventing silent drops at
 * the dispatch boundary (event bus, STOMP publisher).
 */
class MatchEventSealedTest {

  @Test
  void exhaustiveSwitchOverAllNonMoveAppliedVariants() {
    MatchId matchId = MatchId.random();
    Instant ts = Instant.parse("2026-05-02T22:00:00Z");
    UserRef alice = UserRef.anonymousLan("alice");

    // MoveApplied requires Move/GameState fixtures from shared and is exercised by
    // EventSerializationTest and (later) MatchManager tests; here we cover the remaining
    // eight variants. The switch's exhaustiveness is checked across all nine permits at compile
    // time — omitting MoveApplied here would NOT compile. The list elides it because we only
    // need to assert tagging, not construction with full fixtures.
    List<MatchEvent> events =
        List.of(
            new MoveRejected(matchId, 0, ts, alice, RejectionReason.NOT_YOUR_TURN),
            new DrawOffered(matchId, 1, ts, alice),
            new DrawAccepted(matchId, 2, ts),
            new DrawDeclined(matchId, 3, ts),
            new Resigned(matchId, 4, ts, alice),
            new MatchEnded(matchId, 5, ts, MatchResult.WHITE_WINS, EndReason.RESIGN),
            new PlayerDisconnected(matchId, 6, ts, alice),
            new PlayerReconnected(matchId, 7, ts, alice));

    for (MatchEvent e : events) {
      String tag =
          switch (e) {
            case MoveApplied unused -> "MoveApplied";
            case MoveRejected unused -> "MoveRejected";
            case DrawOffered unused -> "DrawOffered";
            case DrawAccepted unused -> "DrawAccepted";
            case DrawDeclined unused -> "DrawDeclined";
            case Resigned unused -> "Resigned";
            case MatchEnded unused -> "MatchEnded";
            case PlayerDisconnected unused -> "PlayerDisconnected";
            case PlayerReconnected unused -> "PlayerReconnected";
          };
      assertThat(tag).isNotEmpty();
    }
  }

  @Test
  void allVariantsExposeMatchIdSequenceAndTimestamp() {
    MatchId mid = MatchId.random();
    Instant ts = Instant.parse("2026-05-02T22:00:00Z");
    DrawOffered evt = new DrawOffered(mid, 42L, ts, UserRef.anonymousLan("a"));

    MatchEvent abstracted = evt;
    assertThat(abstracted.matchId()).isEqualTo(mid);
    assertThat(abstracted.sequenceNo()).isEqualTo(42L);
    assertThat(abstracted.timestamp()).isEqualTo(ts);
  }

  @Test
  void matchEndedRejectsUnfinishedResult() {
    MatchId mid = MatchId.random();
    Instant ts = Instant.now();
    assertThatThrownBy(() -> new MatchEnded(mid, 0, ts, MatchResult.UNFINISHED, EndReason.RESIGN))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("UNFINISHED");
  }

  @Test
  void rejectsNegativeSequenceNo() {
    MatchId mid = MatchId.random();
    assertThatThrownBy(() -> new DrawAccepted(mid, -1, Instant.now()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
