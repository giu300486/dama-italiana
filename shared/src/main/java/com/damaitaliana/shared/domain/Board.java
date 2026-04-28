package com.damaitaliana.shared.domain;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * The 8x8 draughts board, immutable.
 *
 * <p>Internally backed by a 64-element array indexed by {@code rank * 8 + file}. Light (non
 * playable) squares are always {@code null}; pieces only ever live on the 32 dark squares (SPEC
 * §3.1). Each mutator returns a new {@code Board} — instances are safe to share.
 */
public final class Board {

  private static final int SIZE = 64;

  private final Piece[] squares;

  private Board(Piece[] squares) {
    this.squares = squares;
  }

  /** Empty board with no pieces. */
  public static Board empty() {
    return new Board(new Piece[SIZE]);
  }

  /**
   * Initial position (SPEC §3.1).
   *
   * <p>White men occupy the dark squares of ranks 0..2; black men the dark squares of ranks 5..7.
   * Twelve men per side, twenty-four pieces total.
   */
  public static Board initial() {
    Piece whiteMan = new Piece(Color.WHITE, PieceKind.MAN);
    Piece blackMan = new Piece(Color.BLACK, PieceKind.MAN);
    Piece[] arr = new Piece[SIZE];
    for (int rank = 0; rank < 8; rank++) {
      for (int file = 0; file < 8; file++) {
        if ((file + rank) % 2 != 0) {
          continue; // light square
        }
        if (rank <= 2) {
          arr[rank * 8 + file] = whiteMan;
        } else if (rank >= 5) {
          arr[rank * 8 + file] = blackMan;
        }
      }
    }
    return new Board(arr);
  }

  /** Returns the piece on {@code s}, or empty if the square is empty (or light). */
  public Optional<Piece> at(Square s) {
    return Optional.ofNullable(squares[index(s)]);
  }

  /** True iff the square contains no piece. Light squares are always empty. */
  public boolean isEmpty(Square s) {
    return squares[index(s)] == null;
  }

  /**
   * Returns a new board with {@code piece} placed on {@code s}, replacing whatever was there.
   *
   * @throws IllegalArgumentException if {@code s} is light.
   */
  public Board with(Square s, Piece piece) {
    Objects.requireNonNull(piece, "piece");
    if (!s.isDark()) {
      throw new IllegalArgumentException("cannot place a piece on a light square: " + s);
    }
    Piece[] copy = squares.clone();
    copy[index(s)] = piece;
    return new Board(copy);
  }

  /** Returns a new board with {@code s} cleared. Idempotent on already-empty squares. */
  public Board without(Square s) {
    if (squares[index(s)] == null) {
      return this;
    }
    Piece[] copy = squares.clone();
    copy[index(s)] = null;
    return new Board(copy);
  }

  /** Stream of all occupied squares, in row-major order. */
  public Stream<Square> occupied() {
    return IntStream.range(0, SIZE).filter(i -> squares[i] != null).mapToObj(Board::squareOf);
  }

  /** Stream of squares occupied by pieces of the given colour. */
  public Stream<Square> occupiedBy(Color color) {
    Objects.requireNonNull(color, "color");
    return IntStream.range(0, SIZE)
        .filter(i -> squares[i] != null && squares[i].color() == color)
        .mapToObj(Board::squareOf);
  }

  /** Number of pieces of the given colour on the board. */
  public int countPieces(Color color) {
    return (int) occupiedBy(color).count();
  }

  /** Total number of pieces on the board. */
  public int totalPieces() {
    return (int) occupied().count();
  }

  private static int index(Square s) {
    return s.rank() * 8 + s.file();
  }

  private static Square squareOf(int index) {
    return new Square(index % 8, index / 8);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Board other)) {
      return false;
    }
    return Arrays.equals(squares, other.squares);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(squares);
  }

  /**
   * Multi-line ASCII rendering of the board, with rank numbers on the left and file letters at the
   * bottom. Black at the top, White at the bottom. Useful for test diagnostics.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int rank = 7; rank >= 0; rank--) {
      sb.append(rank).append(' ');
      for (int file = 0; file < 8; file++) {
        sb.append(symbolAt(file, rank));
      }
      sb.append('\n');
    }
    sb.append("  abcdefgh\n");
    return sb.toString();
  }

  private char symbolAt(int file, int rank) {
    Piece p = squares[rank * 8 + file];
    if (p == null) {
      return ((file + rank) % 2 == 0) ? '.' : ' ';
    }
    return switch (p.kind()) {
      case MAN -> p.color() == Color.WHITE ? 'w' : 'b';
      case KING -> p.color() == Color.WHITE ? 'W' : 'B';
    };
  }
}
