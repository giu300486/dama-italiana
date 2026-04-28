package com.damaitaliana.shared.notation;

import com.damaitaliana.shared.domain.Square;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Italian Draughts FID notation utility.
 *
 * <h2>Numbering convention (ADR-020 — standard FID orientation)</h2>
 *
 * <p>The 32 dark, playable squares are numbered {@code 1..32} starting from the top-left corner as
 * seen by the White player and proceeding row by row, left to right, towards White's bottom row.
 * White begins the game with men on squares {@code 21..32} and advances them towards the promotion
 * row at squares {@code 1..4}.
 *
 * <pre>
 *  Black side (rank 7)        Square map (numbers shown only on dark squares):
 *
 *      a   b   c   d   e   f   g   h
 *    +---+---+---+---+---+---+---+---+
 *  8 |   | 1 |   | 2 |   | 3 |   | 4 |  rank 7
 *    +---+---+---+---+---+---+---+---+
 *  7 | 5 |   | 6 |   | 7 |   | 8 |   |  rank 6
 *    +---+---+---+---+---+---+---+---+
 *  6 |   | 9 |   |10 |   |11 |   |12 |  rank 5
 *    +---+---+---+---+---+---+---+---+
 *  5 |13 |   |14 |   |15 |   |16 |   |  rank 4
 *    +---+---+---+---+---+---+---+---+
 *  4 |   |17 |   |18 |   |19 |   |20 |  rank 3
 *    +---+---+---+---+---+---+---+---+
 *  3 |21 |   |22 |   |23 |   |24 |   |  rank 2
 *    +---+---+---+---+---+---+---+---+
 *  2 |   |25 |   |26 |   |27 |   |28 |  rank 1
 *    +---+---+---+---+---+---+---+---+
 *  1 |29 |   |30 |   |31 |   |32 |   |  rank 0
 *    +---+---+---+---+---+---+---+---+
 *      White side (rank 0)
 * </pre>
 *
 * <h2>Move format (SPEC §3.8)</h2>
 *
 * <ul>
 *   <li>Simple move: {@code "12-16"} (single hyphen between two numbers).
 *   <li>Capture: {@code "12x19"} (one or more {@code 'x'} separators).
 *   <li>Multi-capture: {@code "12x19x26"}.
 * </ul>
 *
 * <p>This class is stateless. All methods are pure and thread-safe.
 */
public final class FidNotation {

  /** Lowest valid FID square number. */
  public static final int MIN = 1;

  /** Highest valid FID square number. */
  public static final int MAX = 32;

  private static final Pattern SIMPLE = Pattern.compile("^\\d+-\\d+$");
  private static final Pattern CAPTURE = Pattern.compile("^\\d+(?:x\\d+)+$");

  private FidNotation() {}

  /**
   * Converts a {@link Square} to its FID number {@code 1..32}.
   *
   * @throws IllegalArgumentException if the square is light (non-playable).
   */
  public static int toFid(Square s) {
    if (!s.isDark()) {
      throw new IllegalArgumentException("square is light, no FID number: " + s);
    }
    int rowFromTop = 7 - s.rank();
    int colInRow = s.file() / 2;
    return rowFromTop * 4 + colInRow + 1;
  }

  /**
   * Converts a FID number {@code 1..32} to its {@link Square}.
   *
   * @throws IllegalArgumentException if {@code n} is outside {@code [1, 32]}.
   */
  public static Square toSquare(int n) {
    if (n < MIN || n > MAX) {
      throw new IllegalArgumentException("FID number out of range [1,32]: " + n);
    }
    int idx = n - 1;
    int rowFromTop = idx / 4;
    int colInRow = idx % 4;
    int rank = 7 - rowFromTop;
    int file = (rank % 2 == 0) ? 2 * colInRow : 2 * colInRow + 1;
    return new Square(file, rank);
  }

  /** A move parsed from FID notation: a path of square numbers and a flag for capture vs simple. */
  public record ParsedMove(List<Integer> squares, boolean capture) {

    /** Defensive copy of the path; rejects the impossible empty/single-square cases. */
    public ParsedMove {
      if (squares == null || squares.size() < 2) {
        throw new IllegalArgumentException("a move must contain at least two squares");
      }
      squares = List.copyOf(squares);
    }

    /** First square of the move. */
    public int from() {
      return squares.get(0);
    }

    /** Last square of the move. */
    public int to() {
      return squares.get(squares.size() - 1);
    }
  }

  /**
   * Parses a FID-notated move.
   *
   * <p>Accepts {@code "12-16"} (simple) or {@code "12x19[x...]"} (capture). Rejects mixed
   * separators, non-numeric tokens, out-of-range numbers and duplicate consecutive squares.
   *
   * @throws IllegalArgumentException if the input is malformed.
   */
  public static ParsedMove parseMove(String text) {
    if (text == null) {
      throw new IllegalArgumentException("move text is null");
    }
    String trimmed = text.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("move text is empty");
    }

    boolean capture;
    String[] tokens;
    if (SIMPLE.matcher(trimmed).matches()) {
      capture = false;
      tokens = trimmed.split("-");
    } else if (CAPTURE.matcher(trimmed).matches()) {
      capture = true;
      tokens = trimmed.split("x");
    } else {
      throw new IllegalArgumentException("malformed move: " + text);
    }

    List<Integer> squares = new ArrayList<>(tokens.length);
    int previous = -1;
    for (String token : tokens) {
      int n;
      try {
        n = Integer.parseInt(token);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("non-numeric square in move: " + text, e);
      }
      if (n < MIN || n > MAX) {
        throw new IllegalArgumentException("square out of range [1,32] in move " + text + ": " + n);
      }
      if (n == previous) {
        throw new IllegalArgumentException("duplicate consecutive square in move: " + text);
      }
      squares.add(n);
      previous = n;
    }
    return new ParsedMove(squares, capture);
  }

  /**
   * Formats a path of squares in FID notation.
   *
   * @param squares ordered path (size ≥ 2; size = 2 required for a simple move).
   * @param capture {@code true} for {@code 'x'} separators, {@code false} for {@code '-'}.
   * @throws IllegalArgumentException if the inputs are inconsistent (simple move with > 2 squares,
   *     or any square out of range).
   */
  public static String formatMove(List<Integer> squares, boolean capture) {
    if (squares == null || squares.size() < 2) {
      throw new IllegalArgumentException("a move must contain at least two squares");
    }
    if (!capture && squares.size() != 2) {
      throw new IllegalArgumentException(
          "a simple move must have exactly two squares, was " + squares.size());
    }
    String separator = capture ? "x" : "-";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < squares.size(); i++) {
      int n = squares.get(i);
      if (n < MIN || n > MAX) {
        throw new IllegalArgumentException("square out of range [1,32]: " + n);
      }
      if (i > 0) {
        sb.append(separator);
      }
      sb.append(n);
    }
    return sb.toString();
  }
}
