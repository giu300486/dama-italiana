/**
 * Sealed {@code MatchEvent} hierarchy (SPEC §8.3): {@code MoveApplied}, {@code MoveRejected},
 * {@code DrawOffered}, {@code DrawAccepted}, {@code DrawDeclined}, {@code Resigned}, {@code
 * MatchEnded}, {@code PlayerDisconnected}, {@code PlayerReconnected}.
 *
 * <p>All events are immutable records with a monotonic {@code sequenceNo} per match (FR-COM-04,
 * SPEC §7.5) and are Jackson-serializable with the default {@code ObjectMapper} (FR-COM-02 —
 * verified by {@code EventSerializationTest} in Task 4.2).
 *
 * <p>Tournament-level events ({@code TournamentStarted}, {@code BracketUpdated}, {@code
 * StandingsUpdated}, {@code MatchAssigned}, {@code Challenge*}; SPEC §11.4) are deferred to Fase
 * 8/9. Chat events are deferred to Fase 7.
 */
package com.damaitaliana.core.match.event;
