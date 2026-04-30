package com.damaitaliana.client.persistence;

import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.CaptureSequence;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.SimpleMove;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.notation.FidNotation;
import com.damaitaliana.shared.rules.IllegalMoveException;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Jackson-friendly snapshot of a {@link GameState}.
 *
 * <p>The board representation reuses the F1 corpus schema (ADR-022): four disjoint lists of FID
 * square numbers (1..32) for {@code whiteMen}, {@code whiteKings}, {@code blackMen}, {@code
 * blackKings}. {@code sideToMove}, {@code halfmoveClock} and the optional FID-encoded {@code
 * history} round-trip the rest of the state. The {@code status} is not stored: it is recomputed by
 * {@link #toState()} via the {@link RuleEngine}.
 */
public record SerializedGameState(
    List<Integer> whiteMen,
    List<Integer> whiteKings,
    List<Integer> blackMen,
    List<Integer> blackKings,
    Color sideToMove,
    int halfmoveClock,
    List<String> history) {

  private static final RuleEngine RULE_ENGINE = new ItalianRuleEngine();

  /** Defensive copies + null-tolerant defaults. */
  @JsonCreator
  public SerializedGameState(
      @JsonProperty("whiteMen") List<Integer> whiteMen,
      @JsonProperty("whiteKings") List<Integer> whiteKings,
      @JsonProperty("blackMen") List<Integer> blackMen,
      @JsonProperty("blackKings") List<Integer> blackKings,
      @JsonProperty("sideToMove") Color sideToMove,
      @JsonProperty("halfmoveClock") int halfmoveClock,
      @JsonProperty("history") List<String> history) {
    this.whiteMen = whiteMen == null ? List.of() : List.copyOf(whiteMen);
    this.whiteKings = whiteKings == null ? List.of() : List.copyOf(whiteKings);
    this.blackMen = blackMen == null ? List.of() : List.copyOf(blackMen);
    this.blackKings = blackKings == null ? List.of() : List.copyOf(blackKings);
    this.sideToMove = Objects.requireNonNull(sideToMove, "sideToMove");
    if (halfmoveClock < 0) {
      throw new IllegalArgumentException("halfmoveClock must be non-negative: " + halfmoveClock);
    }
    this.halfmoveClock = halfmoveClock;
    this.history = history == null ? List.of() : List.copyOf(history);
  }

  /** Captures {@code state} into the on-disk representation. History is encoded in FID notation. */
  public static SerializedGameState fromState(GameState state) {
    Objects.requireNonNull(state, "state");
    List<Integer> wm = new ArrayList<>();
    List<Integer> wk = new ArrayList<>();
    List<Integer> bm = new ArrayList<>();
    List<Integer> bk = new ArrayList<>();
    state
        .board()
        .occupied()
        .forEach(
            sq -> {
              Piece piece = state.board().at(sq).orElseThrow();
              int fid = FidNotation.toFid(sq);
              boolean white = piece.color() == Color.WHITE;
              boolean man = piece.kind() == PieceKind.MAN;
              if (white && man) {
                wm.add(fid);
              } else if (white) {
                wk.add(fid);
              } else if (man) {
                bm.add(fid);
              } else {
                bk.add(fid);
              }
            });
    List<String> historyFid =
        state.history().stream().map(SerializedGameState::encodeMove).toList();
    return new SerializedGameState(
        wm, wk, bm, bk, state.sideToMove(), state.halfmoveClock(), historyFid);
  }

  /**
   * Materialises this snapshot into a live {@link GameState}.
   *
   * <p>When {@link #history} is empty the snapshot board is trusted directly and the {@link
   * GameStatus} is computed from it. When {@link #history} is non-empty the moves are replayed from
   * {@link GameState#initial()}: this rebuilds the typed {@code List<Move>} that the rule engine
   * needs for repetition detection (SPEC §3.6) and produces a state indistinguishable from one
   * obtained by playing the game live. F3 single-player games always start from the initial
   * position, so replay-from-initial is sound; non-initial roots are out of scope until F4+.
   *
   * @throws IllegalArgumentException if any FID number is out of range, the same square appears in
   *     more than one list, or {@link #history} cannot be replayed from the initial position.
   */
  public GameState toState() {
    Board board = Board.empty();
    boolean[] occupied = new boolean[FidNotation.MAX + 1];
    board = place(board, occupied, whiteMen, Color.WHITE, PieceKind.MAN);
    board = place(board, occupied, whiteKings, Color.WHITE, PieceKind.KING);
    board = place(board, occupied, blackMen, Color.BLACK, PieceKind.MAN);
    board = place(board, occupied, blackKings, Color.BLACK, PieceKind.KING);

    if (history.isEmpty()) {
      GameState ongoing =
          new GameState(board, sideToMove, halfmoveClock, List.of(), GameStatus.ONGOING);
      GameStatus status = RULE_ENGINE.computeStatus(ongoing);
      return new GameState(board, sideToMove, halfmoveClock, List.of(), status);
    }
    GameState cursor = GameState.initial();
    for (String moveText : history) {
      Move move = decodeMove(cursor, moveText);
      try {
        cursor = RULE_ENGINE.applyMove(cursor, move);
      } catch (IllegalMoveException ex) {
        throw new IllegalArgumentException(
            "history move \"" + moveText + "\" is illegal in the saved position", ex);
      }
    }
    return cursor;
  }

  private static Board place(
      Board board, boolean[] occupied, List<Integer> fids, Color color, PieceKind kind) {
    Piece piece = new Piece(color, kind);
    Board cursor = board;
    for (int fid : fids) {
      if (fid < FidNotation.MIN || fid > FidNotation.MAX) {
        throw new IllegalArgumentException("FID out of range [1,32]: " + fid);
      }
      if (occupied[fid]) {
        throw new IllegalArgumentException("FID " + fid + " is listed in more than one piece set");
      }
      occupied[fid] = true;
      cursor = cursor.with(FidNotation.toSquare(fid), piece);
    }
    return cursor;
  }

  private static String encodeMove(Move move) {
    if (move instanceof SimpleMove sm) {
      return FidNotation.formatMove(
          List.of(FidNotation.toFid(sm.from()), FidNotation.toFid(sm.to())), false);
    }
    if (move instanceof CaptureSequence cs) {
      List<Integer> path = new ArrayList<>(cs.path().size() + 1);
      path.add(FidNotation.toFid(cs.from()));
      cs.path().forEach(landing -> path.add(FidNotation.toFid(landing)));
      return FidNotation.formatMove(path, true);
    }
    throw new IllegalStateException("unknown Move subtype: " + move.getClass().getName());
  }

  private static Move decodeMove(GameState state, String text) {
    FidNotation.ParsedMove parsed = FidNotation.parseMove(text);
    Square from = FidNotation.toSquare(parsed.from());
    return RULE_ENGINE.legalMoves(state).stream()
        .filter(m -> m.from().equals(from))
        .filter(m -> matches(m, parsed))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "history move \"" + text + "\" is not legal in the saved position"));
  }

  private static boolean matches(Move move, FidNotation.ParsedMove parsed) {
    List<Integer> path = new ArrayList<>();
    path.add(FidNotation.toFid(move.from()));
    if (move instanceof SimpleMove sm) {
      if (parsed.capture()) {
        return false;
      }
      path.add(FidNotation.toFid(sm.to()));
    } else if (move instanceof CaptureSequence cs) {
      if (!parsed.capture()) {
        return false;
      }
      cs.path().forEach(landing -> path.add(FidNotation.toFid(landing)));
    } else {
      return false;
    }
    return path.equals(parsed.squares());
  }
}
