package com.damaitaliana.core.match.event;

import com.damaitaliana.core.match.MatchId;
import java.time.Instant;

/**
 * Sealed hierarchy of events emitted by the match lifecycle (SPEC §8.3, §11.4).
 *
 * <p>All events share three coordinates:
 *
 * <ul>
 *   <li>{@link #matchId()} — which match the event belongs to.
 *   <li>{@link #sequenceNo()} — strictly monotonic per match (FR-COM-04, SPEC §7.5). Starts at 0.
 *   <li>{@link #timestamp()} — wall-clock {@link Instant} when the event was assigned its sequence.
 * </ul>
 *
 * <p>The hierarchy is sealed: pattern-matching {@code switch} expressions on a {@code MatchEvent}
 * are checked exhaustively at compile time. Callers MUST add a new case when a variant is added,
 * preventing silent drops at the dispatch boundary (event bus, STOMP publisher).
 *
 * <p><b>Wire shape (FR-COM-02 preview)</b>: all variants are Jackson-serializable with the default
 * {@code ObjectMapper} (verified by {@code EventSerializationTest} for the eight variants that
 * depend only on core-server / standard JDK types). {@link MoveApplied} additionally references
 * {@code Move} and {@code GameState} from the {@code shared} module: full wire roundtrip for {@code
 * MoveApplied} is deferred to Fase 6, when the transport layer adds a custom Jackson {@code Module}
 * for the sealed {@code Move} hierarchy and the non-record {@code Board} class.
 */
public sealed interface MatchEvent
    permits MoveApplied,
        MoveRejected,
        DrawOffered,
        DrawAccepted,
        DrawDeclined,
        Resigned,
        MatchEnded,
        PlayerDisconnected,
        PlayerReconnected {

  MatchId matchId();

  long sequenceNo();

  Instant timestamp();
}
