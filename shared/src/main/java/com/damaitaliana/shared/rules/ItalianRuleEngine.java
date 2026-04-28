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
import java.util.HashSet;
import java.util.List;
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
      List<Move> moves = new ArrayList<>(captures.size());
      moves.addAll(captures);
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
    Piece piece =
        state
            .board()
            .at(move.from())
            .orElseThrow(() -> new IllegalMoveException("source square is empty: " + move.from()));

    Board newBoard = state.board().without(move.from());
    for (Square captured : move.capturedSquares()) {
      newBoard = newBoard.without(captured);
    }
    newBoard = newBoard.with(move.to(), piece);

    int newClock = (move.isCapture() || piece.isMan()) ? 0 : state.halfmoveClock() + 1;
    List<Move> history = new ArrayList<>(state.history());
    history.add(move);
    Color nextSide = state.sideToMove().opposite();
    GameState transitional =
        new GameState(newBoard, nextSide, newClock, history, GameStatus.ONGOING);
    GameStatus status = computeStatus(transitional);
    return new GameState(newBoard, nextSide, newClock, history, status);
  }

  @Override
  public GameStatus computeStatus(GameState state) {
    Objects.requireNonNull(state, "state");
    return GameStatus.ONGOING;
  }

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
