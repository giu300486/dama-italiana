package com.damaitaliana.core.match;

import com.damaitaliana.core.eventbus.MatchEventBus;
import com.damaitaliana.core.match.event.DrawAccepted;
import com.damaitaliana.core.match.event.DrawDeclined;
import com.damaitaliana.core.match.event.DrawOffered;
import com.damaitaliana.core.match.event.MatchEnded;
import com.damaitaliana.core.match.event.MatchEvent;
import com.damaitaliana.core.match.event.MoveApplied;
import com.damaitaliana.core.match.event.MoveRejected;
import com.damaitaliana.core.match.event.Resigned;
import com.damaitaliana.core.repository.MatchRepository;
import com.damaitaliana.core.stomp.StompCompatiblePublisher;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.IllegalMoveException;
import com.damaitaliana.shared.rules.RuleEngine;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Match orchestrator (PLAN-fase-4 §4.8): owns {@code createMatch} / {@code applyMove} / {@code
 * resign} / {@code offerDraw} / {@code respondDraw} flows over the {@link MatchRepository} + {@link
 * RuleEngine} ports, broadcasts every applied event on the internal {@link MatchEventBus} and on
 * the {@link StompCompatiblePublisher} topic {@code /topic/match/{id}} (SPEC §11.4).
 *
 * <p>Concurrency: a per-match intrinsic lock serializes all writes to a given match so the strict
 * monotonic sequence-number validation of {@link MatchRepository#appendEvent} cannot fail under
 * normal contention. Reads ({@link #findById}, {@link #eventsSince}) bypass the lock and return the
 * live snapshot from the repository.
 *
 * <p>Anti-cheat (FR-COM-01, SPEC §9.8.3, ADR-040): every {@link MoveRejected} with reason {@link
 * RejectionReason#NOT_YOUR_TURN} or {@link RejectionReason#ILLEGAL_MOVE} bumps the per-player
 * counter on {@link Match}; reaching {@value #ILLEGAL_MOVES_FORFEIT_THRESHOLD} consecutive
 * rejections triggers a {@link MatchEnded} with reason {@link EndReason#FORFEIT_ANTI_CHEAT} and the
 * opponent as winner. {@link RejectionReason#MATCH_NOT_ONGOING} does NOT advance the counter — a
 * stale client retrying after a clean termination is not "cheating" in the same sense.
 */
@Service
public final class MatchManager {

  /** SPEC §9.8.3, FR-COM-01 — 5 consecutive illegal moves trigger forfeit. */
  static final int ILLEGAL_MOVES_FORFEIT_THRESHOLD = 5;

  /** STOMP destination prefix for per-match broadcasts (SPEC §11.4). */
  static final String MATCH_TOPIC_PREFIX = "/topic/match/";

  private final MatchRepository repo;
  private final RuleEngine ruleEngine;
  private final MatchEventBus bus;
  private final StompCompatiblePublisher stompPublisher;
  private final Clock clock;
  private final ConcurrentHashMap<MatchId, Object> matchLocks = new ConcurrentHashMap<>();

  @Autowired
  public MatchManager(
      MatchRepository repo,
      RuleEngine ruleEngine,
      MatchEventBus bus,
      StompCompatiblePublisher stompPublisher) {
    this(repo, ruleEngine, bus, stompPublisher, Clock.systemUTC());
  }

  /** Visible-for-tests constructor that accepts a deterministic {@link Clock}. */
  MatchManager(
      MatchRepository repo,
      RuleEngine ruleEngine,
      MatchEventBus bus,
      StompCompatiblePublisher stompPublisher,
      Clock clock) {
    this.repo = Objects.requireNonNull(repo, "repo");
    this.ruleEngine = Objects.requireNonNull(ruleEngine, "ruleEngine");
    this.bus = Objects.requireNonNull(bus, "bus");
    this.stompPublisher = Objects.requireNonNull(stompPublisher, "stompPublisher");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  // --- Public API -----------------------------------------------------------

  /**
   * Creates a fresh match between {@code white} and {@code black}, persists it, and returns it with
   * status {@link MatchStatus#ONGOING} (Fase 4 starts ongoing immediately; lobby/{@code WAITING}
   * management lands in Fase 6 with the server-centric matchmaking).
   */
  public Match createMatch(UserRef white, UserRef black, TimeControl timeControl) {
    Objects.requireNonNull(white, "white");
    Objects.requireNonNull(black, "black");
    Objects.requireNonNull(timeControl, "timeControl");

    Match match =
        new Match(
            MatchId.random(),
            white,
            black,
            timeControl,
            now(),
            null,
            GameState.initial(),
            -1L,
            MatchStatus.ONGOING,
            null);
    repo.save(match);
    return match;
  }

  /** Lookup pass-through to {@link MatchRepository#findById}. */
  public Optional<Match> findById(MatchId id) {
    Objects.requireNonNull(id, "id");
    return repo.findById(id);
  }

  /** Replay pass-through to {@link MatchRepository#eventsSince}. Lock-free. */
  public List<MatchEvent> eventsSince(MatchId id, long fromSeq) {
    Objects.requireNonNull(id, "id");
    return repo.eventsSince(id, fromSeq);
  }

  /**
   * Validates and applies {@code move} on behalf of {@code sender}. Returns either the {@link
   * MoveApplied} that was broadcast on success, or the {@link MoveRejected} that records the
   * failure cause. On a terminal state (win, draw, anti-cheat forfeit) a {@link MatchEnded} is
   * broadcast as a follow-up event and the match transitions to {@link MatchStatus#FINISHED}.
   *
   * @throws MatchNotFoundException if {@code matchId} has no corresponding match.
   * @throws IllegalArgumentException if {@code sender} is not a participant in the match.
   */
  public MatchEvent applyMove(MatchId matchId, UserRef sender, Move move) {
    Objects.requireNonNull(matchId, "matchId");
    Objects.requireNonNull(sender, "sender");
    Objects.requireNonNull(move, "move");

    synchronized (lockFor(matchId)) {
      Match match = repo.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
      Color senderColor = colorOf(match, sender);

      if (match.status() != MatchStatus.ONGOING) {
        return emit(
            new MoveRejected(
                matchId,
                nextSequenceNo(matchId),
                now(),
                sender,
                RejectionReason.MATCH_NOT_ONGOING));
      }

      if (match.state().sideToMove() != senderColor) {
        MoveRejected rejected =
            emit(
                new MoveRejected(
                    matchId,
                    nextSequenceNo(matchId),
                    now(),
                    sender,
                    RejectionReason.NOT_YOUR_TURN));
        match.recordIllegalMove(senderColor);
        applyAntiCheatCheck(matchId, match, senderColor);
        return rejected;
      }

      GameState newState;
      try {
        newState = ruleEngine.applyMove(match.state(), move);
      } catch (IllegalMoveException illegal) {
        MoveRejected rejected =
            emit(
                new MoveRejected(
                    matchId, nextSequenceNo(matchId), now(), sender, RejectionReason.ILLEGAL_MOVE));
        match.recordIllegalMove(senderColor);
        applyAntiCheatCheck(matchId, match, senderColor);
        return rejected;
      }

      MoveApplied applied =
          emit(new MoveApplied(matchId, nextSequenceNo(matchId), now(), move, newState));
      match.state(newState);
      match.clearIllegalMoves(senderColor);

      if (!newState.status().isOngoing()) {
        emitMatchEnded(
            matchId, match, terminalResult(newState.status()), terminalReason(newState.status()));
      }
      return applied;
    }
  }

  /**
   * Resigns the match on behalf of {@code who}. Emits {@link Resigned} followed by {@link
   * MatchEnded} with reason {@link EndReason#RESIGN} and the opponent as winner. Match transitions
   * to {@link MatchStatus#FINISHED}.
   *
   * @throws MatchNotFoundException if {@code matchId} has no corresponding match.
   * @throws IllegalArgumentException if {@code who} is not a participant.
   * @throws IllegalStateException if the match is not {@link MatchStatus#ONGOING}.
   */
  public MatchEvent resign(MatchId matchId, UserRef who) {
    Objects.requireNonNull(matchId, "matchId");
    Objects.requireNonNull(who, "who");

    synchronized (lockFor(matchId)) {
      Match match = repo.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
      Color resignerColor = colorOf(match, who);

      if (match.status() != MatchStatus.ONGOING) {
        throw new IllegalStateException(
            "Cannot resign on non-ongoing match " + matchId + ": status=" + match.status());
      }

      Resigned resigned = emit(new Resigned(matchId, nextSequenceNo(matchId), now(), who));

      Color winnerColor = resignerColor.opposite();
      MatchResult result =
          winnerColor == Color.WHITE ? MatchResult.WHITE_WINS : MatchResult.BLACK_WINS;
      emitMatchEnded(matchId, match, result, EndReason.RESIGN);
      return resigned;
    }
  }

  /**
   * Offers a draw on behalf of {@code from}. Stores the pending offer on the match so a second
   * offer (from either side) is rejected until the current one is accepted/declined.
   *
   * @throws IllegalStateException if the match is not ongoing or if a draw offer is already
   *     pending.
   */
  public MatchEvent offerDraw(MatchId matchId, UserRef from) {
    Objects.requireNonNull(matchId, "matchId");
    Objects.requireNonNull(from, "from");

    synchronized (lockFor(matchId)) {
      Match match = repo.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
      colorOf(match, from);

      if (match.status() != MatchStatus.ONGOING) {
        throw new IllegalStateException(
            "Cannot offer draw on non-ongoing match " + matchId + ": status=" + match.status());
      }
      if (match.pendingDrawOfferFrom().isPresent()) {
        throw new IllegalStateException("A draw offer is already pending for match " + matchId);
      }

      DrawOffered offered = emit(new DrawOffered(matchId, nextSequenceNo(matchId), now(), from));
      match.pendingDrawOfferFrom(from);
      return offered;
    }
  }

  /**
   * Responds to a pending draw offer. Accepts → {@link DrawAccepted} + {@link MatchEnded}
   * (DRAW_AGREEMENT), match transitions to FINISHED. Declines → {@link DrawDeclined}, match
   * continues ONGOING. The pending offer is cleared in both cases.
   *
   * @throws IllegalStateException if the match is not ongoing, no offer is pending, or the
   *     responder is the same player who issued the offer.
   */
  public MatchEvent respondDraw(MatchId matchId, UserRef responder, boolean accept) {
    Objects.requireNonNull(matchId, "matchId");
    Objects.requireNonNull(responder, "responder");

    synchronized (lockFor(matchId)) {
      Match match = repo.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
      colorOf(match, responder);

      if (match.status() != MatchStatus.ONGOING) {
        throw new IllegalStateException(
            "Cannot respond to draw on non-ongoing match "
                + matchId
                + ": status="
                + match.status());
      }
      UserRef pending =
          match
              .pendingDrawOfferFrom()
              .orElseThrow(
                  () -> new IllegalStateException("No pending draw offer for match " + matchId));
      if (pending.equals(responder)) {
        throw new IllegalStateException(
            "Player " + responder + " cannot respond to their own draw offer");
      }

      if (accept) {
        DrawAccepted accepted = emit(new DrawAccepted(matchId, nextSequenceNo(matchId), now()));
        match.pendingDrawOfferFrom(null);
        emitMatchEnded(matchId, match, MatchResult.DRAW, EndReason.DRAW_AGREEMENT);
        return accepted;
      }
      DrawDeclined declined = emit(new DrawDeclined(matchId, nextSequenceNo(matchId), now()));
      match.pendingDrawOfferFrom(null);
      return declined;
    }
  }

  // --- Helpers --------------------------------------------------------------

  private void applyAntiCheatCheck(MatchId matchId, Match match, Color senderColor) {
    if (match.consecutiveIllegalMoves(senderColor) >= ILLEGAL_MOVES_FORFEIT_THRESHOLD) {
      Color winnerColor = senderColor.opposite();
      MatchResult result =
          winnerColor == Color.WHITE ? MatchResult.WHITE_WINS : MatchResult.BLACK_WINS;
      emitMatchEnded(matchId, match, result, EndReason.FORFEIT_ANTI_CHEAT);
    }
  }

  private void emitMatchEnded(MatchId matchId, Match match, MatchResult result, EndReason reason) {
    emit(new MatchEnded(matchId, nextSequenceNo(matchId), now(), result, reason));
    match.status(MatchStatus.FINISHED);
  }

  private <E extends MatchEvent> E emit(E event) {
    repo.appendEvent(event);
    bus.publish(event);
    stompPublisher.publishToTopic(MATCH_TOPIC_PREFIX + event.matchId(), event);
    return event;
  }

  private long nextSequenceNo(MatchId matchId) {
    return repo.currentSequenceNo(matchId) + 1L;
  }

  private Object lockFor(MatchId id) {
    return matchLocks.computeIfAbsent(id, k -> new Object());
  }

  private Instant now() {
    return Instant.now(clock);
  }

  private static Color colorOf(Match match, UserRef who) {
    if (who.equals(match.white())) {
      return Color.WHITE;
    }
    if (who.equals(match.black())) {
      return Color.BLACK;
    }
    throw new IllegalArgumentException(
        "User " + who + " is not a participant in match " + match.id());
  }

  private static MatchResult terminalResult(GameStatus gameStatus) {
    return switch (gameStatus) {
      case ONGOING -> throw new IllegalArgumentException("ONGOING is not a terminal status");
      case WHITE_WINS -> MatchResult.WHITE_WINS;
      case BLACK_WINS -> MatchResult.BLACK_WINS;
      case DRAW_REPETITION, DRAW_FORTY_MOVES, DRAW_AGREEMENT -> MatchResult.DRAW;
    };
  }

  private static EndReason terminalReason(GameStatus gameStatus) {
    return switch (gameStatus) {
      case ONGOING -> throw new IllegalArgumentException("ONGOING is not a terminal status");
      case WHITE_WINS, BLACK_WINS -> EndReason.CHECKMATE_LIKE;
      case DRAW_REPETITION -> EndReason.DRAW_REPETITION;
      case DRAW_FORTY_MOVES -> EndReason.DRAW_FORTY_MOVES;
      case DRAW_AGREEMENT -> EndReason.DRAW_AGREEMENT;
    };
  }
}
