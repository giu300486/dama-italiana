package com.damaitaliana.core.match.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.core.match.EndReason;
import com.damaitaliana.core.match.MatchId;
import com.damaitaliana.core.match.MatchResult;
import com.damaitaliana.core.match.RejectionReason;
import com.damaitaliana.core.match.UserRef;
import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.SimpleMove;
import com.damaitaliana.shared.domain.Square;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Smoke test for FR-COM-02 (preview). Verifies that the eight {@link MatchEvent} variants that
 * depend only on {@code core-server} and standard JDK types roundtrip cleanly through the default
 * {@link ObjectMapper} (with {@link JavaTimeModule} registered for {@link Instant}).
 *
 * <p>The mapper disables {@link MapperFeature#AUTO_DETECT_IS_GETTERS} so that {@link
 * com.damaitaliana.core.match.UserRef#isAnonymous()} (a derived helper, not a record component) is
 * not picked up as the synthetic property {@code "anonymous"} during serialization — without this,
 * deser fails on the unexpected field. The setting affects only the test mapper; the wire layer
 * (Fase 6) brings its own configuration.
 *
 * <p>{@link MoveApplied} is NOT exercised through Jackson here. Its component {@code newState} of
 * type {@code GameState} contains a {@code Board} (non-record class with a private final {@code
 * Piece[]} field) and a {@code Move} (sealed interface) — both require a custom Jackson {@code
 * Module} that lands in Fase 6 with the server transport layer. {@link
 * #moveAppliedConstructsWithExpectedComponents()} below covers the constructor-side smoke.
 */
class EventSerializationTest {

  private final ObjectMapper mapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(MapperFeature.AUTO_DETECT_IS_GETTERS);

  private static final MatchId MATCH = MatchId.of("11111111-2222-3333-4444-555555555555");
  private static final Instant TS = Instant.parse("2026-05-02T22:00:00Z");
  private static final UserRef ALICE = UserRef.anonymousLan("alice");

  @Test
  void moveRejectedRoundtrip() throws Exception {
    var event = new MoveRejected(MATCH, 0L, TS, ALICE, RejectionReason.NOT_YOUR_TURN);
    var back = mapper.readValue(mapper.writeValueAsString(event), MoveRejected.class);
    assertThat(back).isEqualTo(event);
  }

  @Test
  void drawOfferedRoundtrip() throws Exception {
    var event = new DrawOffered(MATCH, 1L, TS, ALICE);
    var back = mapper.readValue(mapper.writeValueAsString(event), DrawOffered.class);
    assertThat(back).isEqualTo(event);
  }

  @Test
  void drawAcceptedRoundtrip() throws Exception {
    var event = new DrawAccepted(MATCH, 2L, TS);
    var back = mapper.readValue(mapper.writeValueAsString(event), DrawAccepted.class);
    assertThat(back).isEqualTo(event);
  }

  @Test
  void drawDeclinedRoundtrip() throws Exception {
    var event = new DrawDeclined(MATCH, 3L, TS);
    var back = mapper.readValue(mapper.writeValueAsString(event), DrawDeclined.class);
    assertThat(back).isEqualTo(event);
  }

  @Test
  void resignedRoundtrip() throws Exception {
    var event = new Resigned(MATCH, 4L, TS, ALICE);
    var back = mapper.readValue(mapper.writeValueAsString(event), Resigned.class);
    assertThat(back).isEqualTo(event);
  }

  @Test
  void matchEndedRoundtrip() throws Exception {
    var event = new MatchEnded(MATCH, 5L, TS, MatchResult.WHITE_WINS, EndReason.FORFEIT_ANTI_CHEAT);
    var back = mapper.readValue(mapper.writeValueAsString(event), MatchEnded.class);
    assertThat(back).isEqualTo(event);
  }

  @Test
  void playerDisconnectedRoundtrip() throws Exception {
    var event = new PlayerDisconnected(MATCH, 6L, TS, ALICE);
    var back = mapper.readValue(mapper.writeValueAsString(event), PlayerDisconnected.class);
    assertThat(back).isEqualTo(event);
  }

  @Test
  void playerReconnectedRoundtrip() throws Exception {
    var event = new PlayerReconnected(MATCH, 7L, TS, ALICE);
    var back = mapper.readValue(mapper.writeValueAsString(event), PlayerReconnected.class);
    assertThat(back).isEqualTo(event);
  }

  /**
   * Constructor-side smoke for {@link MoveApplied}. Full Jackson roundtrip (and the wire shape per
   * SPEC §11.4) is deferred to Fase 6 with a custom {@code Module} for the sealed {@code Move}
   * hierarchy and the non-record {@code Board}.
   */
  @Test
  void moveAppliedConstructsWithExpectedComponents() {
    GameState state = new GameState(Board.initial(), Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    var move = new SimpleMove(new Square(0, 2), new Square(1, 3));
    var event = new MoveApplied(MATCH, 0L, TS, move, state);

    assertThat(event.matchId()).isEqualTo(MATCH);
    assertThat(event.sequenceNo()).isZero();
    assertThat(event.timestamp()).isEqualTo(TS);
    assertThat(event.move()).isEqualTo(move);
    assertThat(event.newState()).isEqualTo(state);
  }
}
