package com.damaitaliana.core.match;

import com.damaitaliana.core.tournament.TournamentMatchRef;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import java.time.Instant;
import java.util.EnumMap;
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
 * <p><b>Order deviation from PLAN-fase-4</b>: the data shape of this class was originally scheduled
 * for Task 4.7 but was anticipated to Task 4.3 because {@link
 * com.damaitaliana.core.repository.MatchRepository} ports require {@code Match} as a typed argument
 * and the in-memory adapters of Task 4.4 cannot compile against an undefined type. Task 4.7 added
 * the state-machine validation in {@link #status(MatchStatus)} and the anti-cheat counter ({@link
 * #consecutiveIllegalMoves(Color)}, {@link #recordIllegalMove(Color)}, {@link
 * #clearIllegalMoves(Color)}). The wiring — when to call which mutator — lives in {@link
 * MatchManager} (Task 4.8).
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
   * Anti-cheat counter (FR-COM-01, SPEC §9.8.3) — consecutive illegal-move attempts per side. Reset
   * on a legal move; reaching 5 triggers a forfeit (handled by {@link MatchManager} in Task 4.8).
   * Held in-memory only, NOT persisted, NOT part of the wire shape — recovery (Fase 6 reconnection)
   * resets the counter to zero (ADR-040, Task 4.13). The map is initialized with both colors at 0
   * so {@link #consecutiveIllegalMoves(Color)} never returns {@code null}.
   */
  private final EnumMap<Color, Integer> consecutiveIllegalMoves;

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
    this.consecutiveIllegalMoves = new EnumMap<>(Color.class);
    this.consecutiveIllegalMoves.put(Color.WHITE, 0);
    this.consecutiveIllegalMoves.put(Color.BLACK, 0);
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

  /**
   * Returns the consecutive illegal-move count for {@code who}. Always non-negative. Reset to 0
   * after every legal move applied by {@code who} (handled by {@link MatchManager}, Task 4.8).
   */
  public int consecutiveIllegalMoves(Color who) {
    Objects.requireNonNull(who, "who");
    return consecutiveIllegalMoves.get(who);
  }

  // --- Package-private mutators wired by MatchManager (Task 4.8). -----------

  void state(GameState newState) {
    this.state = Objects.requireNonNull(newState, "newState");
  }

  /**
   * Transitions to {@code newStatus} after validating the move against the state machine
   * (PLAN-fase-4 §4.7): {@code WAITING → ONGOING}, {@code ONGOING → FINISHED}, {@code ONGOING →
   * ABORTED}. Any other transition (including identity, regression, or out-of-terminal) throws
   * {@link IllegalStateException}.
   *
   * @throws IllegalStateException if {@code newStatus} is not a legal next state from the current
   *     {@link #status()}.
   */
  void status(MatchStatus newStatus) {
    Objects.requireNonNull(newStatus, "newStatus");
    if (!isLegalTransition(this.status, newStatus)) {
      throw new IllegalStateException(
          "invalid match status transition: " + this.status + " -> " + newStatus);
    }
    this.status = newStatus;
  }

  void recordIllegalMove(Color who) {
    Objects.requireNonNull(who, "who");
    consecutiveIllegalMoves.merge(who, 1, Integer::sum);
  }

  void clearIllegalMoves(Color who) {
    Objects.requireNonNull(who, "who");
    consecutiveIllegalMoves.put(who, 0);
  }

  private static boolean isLegalTransition(MatchStatus from, MatchStatus to) {
    return switch (from) {
      case WAITING -> to == MatchStatus.ONGOING;
      case ONGOING -> to == MatchStatus.FINISHED || to == MatchStatus.ABORTED;
      case FINISHED, ABORTED -> false;
    };
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
