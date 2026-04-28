package com.damaitaliana.shared.rules;

import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.CaptureSequence;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.SimpleMove;
import com.damaitaliana.shared.domain.Square;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Reference Italian Draughts rule engine.
 *
 * <p>Implements the rules of SPEC §3 — and ONLY those.
 *
 * <ul>
 *   <li>Task 1.3 — non-capturing movement of men and kings.
 *   <li>Task 1.4 — single captures with the man-cannot-capture-king rule (SPEC §3.3) and the
 *       mandatory-capture rule.
 *   <li>Task 1.5+ — multi-jump capture sequences and the four laws of precedence.
 * </ul>
 */
public final class ItalianRuleEngine implements RuleEngine {

  private static final int[][] KING_DIRS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

  @Override
  public List<Move> legalMoves(GameState state) {
    Objects.requireNonNull(state, "state");
    if (!state.status().isOngoing()) {
      return List.of();
    }
    Board board = state.board();
    Color side = state.sideToMove();

    List<CaptureSequence> captures = new ArrayList<>();
    board
        .occupiedBy(side)
        .forEach(
            from -> {
              Piece p = board.at(from).orElseThrow();
              captures.addAll(generateCaptureSequencesFor(board, from, p));
            });

    if (!captures.isEmpty()) {
      // SPEC §3.3: capture is mandatory whenever possible. Simple moves are excluded.
      // SPEC §3.4: filter captures through the four Italian laws of precedence.
      List<CaptureSequence> filtered = applyFourLaws(board, captures);
      List<Move> moves = new ArrayList<>(filtered.size());
      moves.addAll(filtered);
      return List.copyOf(moves);
    }

    List<Move> simpleMoves = new ArrayList<>();
    board
        .occupiedBy(side)
        .forEach(
            from -> {
              Piece p = board.at(from).orElseThrow();
              simpleMoves.addAll(generateSimpleMovesFor(board, from, p));
            });
    return List.copyOf(simpleMoves);
  }

  @Override
  public GameState applyMove(GameState state, Move move) {
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(move, "move");
    List<Move> legal = legalMoves(state);
    if (!legal.contains(move)) {
      throw new IllegalMoveException("move not legal in current position: " + move);
    }
    GameState transitional = applyCore(state, move);
    GameStatus status = computeStatus(transitional);
    return new GameState(
        transitional.board(),
        transitional.sideToMove(),
        transitional.halfmoveClock(),
        transitional.history(),
        status);
  }

  /**
   * Pure board-state transition for {@code move}, without recomputing the game status. Reused by
   * {@link #applyMove} (via the public path that adds the status) and by {@link
   * #isThreefoldRepetition} when replaying history. The move is assumed to be legal.
   */
  private GameState applyCore(GameState state, Move move) {
    Piece piece =
        state
            .board()
            .at(move.from())
            .orElseThrow(() -> new IllegalMoveException("source square is empty: " + move.from()));

    Board newBoard = state.board().without(move.from());
    for (Square captured : move.capturedSquares()) {
      newBoard = newBoard.without(captured);
    }
    // SPEC §3.5 — a man that ends its move on the opponent's last row is promoted to king.
    Piece pieceAfterMove = piece;
    if (piece.isMan() && move.to().rank() == promotionRank(piece.color())) {
      pieceAfterMove = piece.promote();
    }
    newBoard = newBoard.with(move.to(), pieceAfterMove);

    // SPEC §3.6 — clock resets on captures and on man moves; otherwise increments.
    int newClock = (move.isCapture() || piece.isMan()) ? 0 : state.halfmoveClock() + 1;
    List<Move> history = new ArrayList<>(state.history());
    history.add(move);
    Color nextSide = state.sideToMove().opposite();
    return new GameState(newBoard, nextSide, newClock, history, GameStatus.ONGOING);
  }

  @Override
  public GameStatus computeStatus(GameState state) {
    Objects.requireNonNull(state, "state");
    Color side = state.sideToMove();

    // SPEC §3.6 — opponent without pieces is a loss (no pieces left to move).
    if (state.board().countPieces(side) == 0) {
      return side == Color.WHITE ? GameStatus.BLACK_WINS : GameStatus.WHITE_WINS;
    }

    // SPEC §3.6 — stalemate (side to move has no legal moves) is a LOSS in the Italian variant.
    GameState ongoingView =
        state.status().isOngoing()
            ? state
            : new GameState(
                state.board(),
                state.sideToMove(),
                state.halfmoveClock(),
                state.history(),
                GameStatus.ONGOING);
    if (legalMoves(ongoingView).isEmpty()) {
      return side == Color.WHITE ? GameStatus.BLACK_WINS : GameStatus.WHITE_WINS;
    }

    // SPEC §3.6 — 40 moves (= 80 half-moves) without captures or man moves is a draw.
    if (state.halfmoveClock() >= 80) {
      return GameStatus.DRAW_FORTY_MOVES;
    }

    // SPEC §3.6 — threefold repetition is a draw.
    if (isThreefoldRepetition(state)) {
      return GameStatus.DRAW_REPETITION;
    }

    return GameStatus.ONGOING;
  }

  /**
   * Returns {@code true} iff the (board, side-to-move) of {@code current} has appeared at least
   * three times across the trajectory from the initial position to {@code current}.
   *
   * <p>Replays the move history from {@link GameState#initial()} via {@link #applyCore} (no status
   * recomputation, no recursion) and counts occurrences of each {@link PositionKey}. The caller is
   * responsible for guaranteeing that {@code current.history()} is consistent with the standard
   * initial position.
   */
  private boolean isThreefoldRepetition(GameState current) {
    List<Move> history = current.history();
    if (history.size() < 4) {
      // Need at least 4 plies for any single position to recur three times.
      return false;
    }
    Map<PositionKey, Integer> counts = new HashMap<>();
    GameState st = GameState.initial();
    counts.merge(new PositionKey(st.board(), st.sideToMove()), 1, Integer::sum);
    for (Move m : history) {
      st = applyCore(st, m);
      counts.merge(new PositionKey(st.board(), st.sideToMove()), 1, Integer::sum);
    }
    Integer occurrences = counts.get(new PositionKey(current.board(), current.sideToMove()));
    return occurrences != null && occurrences >= 3;
  }

  /** Identity of a position for repetition purposes (ADR-021). */
  private record PositionKey(Board board, Color sideToMove) {}

  // --- private helpers ---

  private static List<Move> generateSimpleMovesFor(Board board, Square from, Piece piece) {
    int[][] dirs = piece.isKing() ? KING_DIRS : manDirs(piece.color());
    List<Move> moves = new ArrayList<>();
    for (int[] d : dirs) {
      Optional<Square> to = offset(from, d[0], d[1]);
      if (to.isPresent() && board.isEmpty(to.get())) {
        moves.add(new SimpleMove(from, to.get()));
      }
    }
    return moves;
  }

  /**
   * Generates every legal capture sequence starting at {@code from}, exploring multi-jump paths via
   * DFS.
   *
   * <p>SPEC §3.3 (man cannot capture king) is enforced at every leg. SPEC §3.5 (a man that reaches
   * the promotion row mid-sequence stops there) terminates the recursion as soon as the landing
   * square is on the man's promotion rank.
   *
   * <p>The same enemy piece is never jumped twice: the in-flight {@link Set} of captured squares is
   * consulted before every leg.
   */
  private static List<CaptureSequence> generateCaptureSequencesFor(
      Board board, Square from, Piece piece) {
    List<CaptureSequence> output = new ArrayList<>();
    dfsCapture(
        board, from, piece, from, new ArrayList<>(), new ArrayList<>(), new HashSet<>(), output);
    return output;
  }

  private static void dfsCapture(
      Board board,
      Square current,
      Piece piece,
      Square origin,
      List<Square> pathSoFar,
      List<Square> capturedSoFar,
      Set<Square> capturedSet,
      List<CaptureSequence> output) {

    int[][] dirs = piece.isKing() ? KING_DIRS : manDirs(piece.color());
    for (int[] d : dirs) {
      Optional<Square> adj = offset(current, d[0], d[1]);
      if (adj.isEmpty() || capturedSet.contains(adj.get())) {
        continue;
      }
      Optional<Piece> adjPiece = board.at(adj.get());
      if (adjPiece.isEmpty() || adjPiece.get().color() == piece.color()) {
        continue;
      }
      // SPEC §3.3 — a man cannot capture a king.
      if (piece.isMan() && adjPiece.get().isKing()) {
        continue;
      }
      Optional<Square> landing = offset(adj.get(), d[0], d[1]);
      if (landing.isEmpty() || !isVirtuallyEmpty(board, landing.get(), origin, capturedSet)) {
        continue;
      }

      pathSoFar.add(landing.get());
      capturedSoFar.add(adj.get());
      capturedSet.add(adj.get());

      // SPEC §3.5 — a man that reaches the promotion row mid-sequence stops.
      boolean stopAtPromotion =
          piece.isMan() && landing.get().rank() == promotionRank(piece.color());

      if (stopAtPromotion) {
        output.add(new CaptureSequence(origin, List.copyOf(pathSoFar), List.copyOf(capturedSoFar)));
      } else {
        int outputSizeBefore = output.size();
        dfsCapture(
            board, landing.get(), piece, origin, pathSoFar, capturedSoFar, capturedSet, output);
        if (output.size() == outputSizeBefore) {
          // No further extension was possible: this leaf is a complete sequence.
          output.add(
              new CaptureSequence(origin, List.copyOf(pathSoFar), List.copyOf(capturedSoFar)));
        }
      }

      pathSoFar.remove(pathSoFar.size() - 1);
      capturedSoFar.remove(capturedSoFar.size() - 1);
      capturedSet.remove(adj.get());
    }
  }

  private static boolean isVirtuallyEmpty(
      Board board, Square s, Square origin, Set<Square> capturedSet) {
    if (s.equals(origin)) {
      // Origin is "empty" during the sequence because the moving piece left it on jump 1.
      return true;
    }
    if (capturedSet.contains(s)) {
      // Already captured this leg → square is empty for landing purposes.
      return true;
    }
    return board.isEmpty(s);
  }

  private static int promotionRank(Color color) {
    return color == Color.WHITE ? 7 : 0;
  }

  // --- SPEC §3.4 — four Italian laws of precedence ---

  /**
   * Applies the four Italian laws of precedence in order: quantity, quality, king-precedence,
   * first-king (SPEC §3.4). Each law is a strict filter: it keeps only the sequences that achieve
   * the optimum on the dimension it ranks, and never reorders the input.
   */
  private static List<CaptureSequence> applyFourLaws(Board board, List<CaptureSequence> all) {
    if (all.size() <= 1) {
      return all;
    }
    List<CaptureSequence> kept = applyQuantityLaw(all);
    kept = applyQualityLaw(board, kept);
    kept = applyKingPrecedenceLaw(board, kept);
    kept = applyFirstKingLaw(board, kept);
    return kept;
  }

  /** Law 1 — keep only the sequences that capture the largest number of pieces. */
  private static List<CaptureSequence> applyQuantityLaw(List<CaptureSequence> sequences) {
    int max = 0;
    for (CaptureSequence s : sequences) {
      if (s.captureCount() > max) {
        max = s.captureCount();
      }
    }
    List<CaptureSequence> kept = new ArrayList<>();
    for (CaptureSequence s : sequences) {
      if (s.captureCount() == max) {
        kept.add(s);
      }
    }
    return kept;
  }

  /**
   * Law 2 — keep only the sequences that capture the largest number of kings. Tie-breaker applied
   * after the quantity law, so all candidates already capture the same total number of pieces.
   */
  private static List<CaptureSequence> applyQualityLaw(
      Board board, List<CaptureSequence> sequences) {
    int max = 0;
    for (CaptureSequence s : sequences) {
      int kings = countKingsCaptured(board, s);
      if (kings > max) {
        max = kings;
      }
    }
    List<CaptureSequence> kept = new ArrayList<>();
    for (CaptureSequence s : sequences) {
      if (countKingsCaptured(board, s) == max) {
        kept.add(s);
      }
    }
    return kept;
  }

  /**
   * Law 3 — if at least one sequence is performed by a king, drop those performed by a man.
   * Otherwise leave the list unchanged.
   */
  private static List<CaptureSequence> applyKingPrecedenceLaw(
      Board board, List<CaptureSequence> sequences) {
    boolean anyFromKing = false;
    for (CaptureSequence s : sequences) {
      if (board.at(s.from()).map(Piece::isKing).orElse(false)) {
        anyFromKing = true;
        break;
      }
    }
    if (!anyFromKing) {
      return sequences;
    }
    List<CaptureSequence> kept = new ArrayList<>();
    for (CaptureSequence s : sequences) {
      if (board.at(s.from()).map(Piece::isKing).orElse(false)) {
        kept.add(s);
      }
    }
    return kept;
  }

  /**
   * Law 4 — among the surviving sequences (all from a king, same total / king count), if at least
   * one captures an enemy king at the first jump, drop those that don't.
   */
  private static List<CaptureSequence> applyFirstKingLaw(
      Board board, List<CaptureSequence> sequences) {
    boolean anyKingFirst = false;
    for (CaptureSequence s : sequences) {
      if (firstCapturedIsKing(board, s)) {
        anyKingFirst = true;
        break;
      }
    }
    if (!anyKingFirst) {
      return sequences;
    }
    List<CaptureSequence> kept = new ArrayList<>();
    for (CaptureSequence s : sequences) {
      if (firstCapturedIsKing(board, s)) {
        kept.add(s);
      }
    }
    return kept;
  }

  private static int countKingsCaptured(Board board, CaptureSequence seq) {
    int n = 0;
    for (Square sq : seq.captured()) {
      Optional<Piece> p = board.at(sq);
      if (p.isPresent() && p.get().isKing()) {
        n++;
      }
    }
    return n;
  }

  private static boolean firstCapturedIsKing(Board board, CaptureSequence seq) {
    return board.at(seq.captured().get(0)).map(Piece::isKing).orElse(false);
  }

  private static int[][] manDirs(Color color) {
    int dr = (color == Color.WHITE) ? +1 : -1;
    return new int[][] {{-1, dr}, {1, dr}};
  }

  private static Optional<Square> offset(Square from, int df, int dr) {
    int f = from.file() + df;
    int r = from.rank() + dr;
    if (f < 0 || f > 7 || r < 0 || r > 7) {
      return Optional.empty();
    }
    return Optional.of(new Square(f, r));
  }
}
