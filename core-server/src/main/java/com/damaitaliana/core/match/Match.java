package com.damaitaliana.core.match;

import com.damaitaliana.core.tournament.TournamentMatchRef;
import com.damaitaliana.shared.domain.GameState;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Match aggregate (SPEC §8.3) — mutable in-memory state for the lifecycle of a single match.
 *
 * <p>Identity ({@code id}, {@code white}, {@code black}, {@code timeControl}, {@code startedAt},
 * {@code tournamentRef}) is immutable; transient state ({@code state}, {@code currentSequenceNo},
 * {@code status}, {@code pendingDrawOfferFrom}) mutates during the match via package-private
 * setters that {@link com.damaitaliana.core.match.MatchManager} (Task 4.8) calls.
 *
 * <p><b>Order deviation from PLAN-fase-4</b>: this class was originally scheduled for Task 4.7. It
 * was anticipated to Task 4.3 because {@link com.damaitaliana.core.repository.MatchRepository}
 * ports require {@code Match} as a typed argument and the in-memory adapters of Task 4.4 cannot
 * compile against an undefined type. The state-machine wiring (anti-cheat counter, transitions)
 * stays in Task 4.8 — {@code Match} itself is intentionally a thin data carrier with structural
 * validation only.
 *
 * <p>Optional fields ({@code tournamentRef}, {@code pendingDrawOfferFrom}) are stored as nullable
 * references and exposed via {@link Optional} accessors per CLAUDE.md §4.1 ("Optional solo per
 * ritorni; mai per parametri o campi").
 */
public final class Match {

  private final MatchId id;
  private final UserRef white;
  private final UserRef black;
  private final TimeControl timeControl;
  private final Instant startedAt;

  /** Nullable: present only for matches scheduled by a tournament. */
  private final TournamentMatchRef tournamentRef;

  // Mutable transient state — package-private setters used by MatchManager (Task 4.8).
  private GameState state;
  private long currentSequenceNo;
  private MatchStatus status;

  /** Nullable: set when one side offers a draw, cleared on accept/decline. */
  private UserRef pendingDrawOfferFrom;

  /**
   * Full constructor.
   *
   * @param tournamentRef may be {@code null} for free matches outside a tournament.
   * @param pendingDrawOfferFrom typically {@code null} at construction; may be set later.
   */
  public Match(
      MatchId id,
      UserRef white,
      UserRef black,
      TimeControl timeControl,
      Instant startedAt,
      TournamentMatchRef tournamentRef,
      GameState state,
      long currentSequenceNo,
      MatchStatus status,
      UserRef pendingDrawOfferFrom) {
    this.id = Objects.requireNonNull(id, "id");
    this.white = Objects.requireNonNull(white, "white");
    this.black = Objects.requireNonNull(black, "black");
    this.timeControl = Objects.requireNonNull(timeControl, "timeControl");
    this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
    this.state = Objects.requireNonNull(state, "state");
    this.status = Objects.requireNonNull(status, "status");
    if (currentSequenceNo < -1L) {
      throw new IllegalArgumentException(
          "currentSequenceNo must be >= -1 (-1 means no events yet), got: " + currentSequenceNo);
    }
    this.currentSequenceNo = currentSequenceNo;
    this.tournamentRef = tournamentRef;
    this.pendingDrawOfferFrom = pendingDrawOfferFrom;
  }

  public MatchId id() {
    return id;
  }

  public UserRef white() {
    return white;
  }

  public UserRef black() {
    return black;
  }

  public TimeControl timeControl() {
    return timeControl;
  }

  public Instant startedAt() {
    return startedAt;
  }

  public Optional<TournamentMatchRef> tournamentRef() {
    return Optional.ofNullable(tournamentRef);
  }

  public GameState state() {
    return state;
  }

  public long currentSequenceNo() {
    return currentSequenceNo;
  }

  public MatchStatus status() {
    return status;
  }

  public Optional<UserRef> pendingDrawOfferFrom() {
    return Optional.ofNullable(pendingDrawOfferFrom);
  }

  // --- Package-private mutators wired by MatchManager (Task 4.8). -----------

  void state(GameState newState) {
    this.state = Objects.requireNonNull(newState, "newState");
  }

  void status(MatchStatus newStatus) {
    this.status = Objects.requireNonNull(newStatus, "newStatus");
  }

  void currentSequenceNo(long newSeq) {
    if (newSeq < -1L) {
      throw new IllegalArgumentException("currentSequenceNo must be >= -1, got: " + newSeq);
    }
    this.currentSequenceNo = newSeq;
  }

  void pendingDrawOfferFrom(UserRef who) {
    this.pendingDrawOfferFrom = who;
  }
}
