package com.damaitaliana.shared.rules;

import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.CaptureSequence;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.SimpleMove;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.notation.FidNotation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Loads the parametric Italian Draughts rule corpus from {@code test-positions.json} and exposes
 * helpers to convert {@link Position} entries into {@link com.damaitaliana.shared.domain.GameState}
 * instances and to render rule-engine moves back into FID notation.
 */
final class CorpusLoader {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private CorpusLoader() {}

  /** Loads {@code /test-positions.json} from the classpath. */
  static CorpusFile loadDefault() {
    try (InputStream in =
        CorpusLoader.class.getClassLoader().getResourceAsStream("test-positions.json")) {
      if (in == null) {
        throw new IllegalStateException("test-positions.json not found on the classpath");
      }
      return MAPPER.readValue(in, CorpusFile.class);
    } catch (IOException e) {
      throw new IllegalStateException("failed to load test-positions.json", e);
    }
  }

  /** Builds a {@link Board} from the disjoint piece-list specification. */
  static Board buildBoard(BoardSpec spec) {
    Board b = Board.empty();
    Piece whiteMan = new Piece(Color.WHITE, PieceKind.MAN);
    Piece whiteKing = new Piece(Color.WHITE, PieceKind.KING);
    Piece blackMan = new Piece(Color.BLACK, PieceKind.MAN);
    Piece blackKing = new Piece(Color.BLACK, PieceKind.KING);
    Set<Integer> seen = new java.util.HashSet<>();
    for (List<Integer> list :
        List.of(spec.whiteMen(), spec.whiteKings(), spec.blackMen(), spec.blackKings())) {
      for (int n : list) {
        if (!seen.add(n)) {
          throw new IllegalArgumentException("duplicate FID square in board spec: " + n);
        }
      }
    }
    for (int n : spec.whiteMen()) {
      b = b.with(FidNotation.toSquare(n), whiteMan);
    }
    for (int n : spec.whiteKings()) {
      b = b.with(FidNotation.toSquare(n), whiteKing);
    }
    for (int n : spec.blackMen()) {
      b = b.with(FidNotation.toSquare(n), blackMan);
    }
    for (int n : spec.blackKings()) {
      b = b.with(FidNotation.toSquare(n), blackKing);
    }
    return b;
  }

  /** Renders a {@link Move} as a FID-notation string ({@code "12-16"} or {@code "12x19x26"}). */
  static String formatMove(Move move) {
    if (move instanceof SimpleMove sm) {
      List<Integer> squares = List.of(FidNotation.toFid(sm.from()), FidNotation.toFid(sm.to()));
      return FidNotation.formatMove(squares, false);
    }
    CaptureSequence cs = (CaptureSequence) move;
    List<Integer> squares = new ArrayList<>();
    squares.add(FidNotation.toFid(cs.from()));
    for (Square s : cs.path()) {
      squares.add(FidNotation.toFid(s));
    }
    return FidNotation.formatMove(squares, true);
  }

  // --- DTOs (Jackson) ---

  @JsonIgnoreProperties(ignoreUnknown = true)
  record CorpusFile(int version, List<Position> positions) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Position(
      String id,
      String description,
      String specReference,
      String category,
      BoardSpec board,
      String sideToMove,
      List<String> expectedLegalMoves,
      List<String> rejectedMoves,
      String notes) {

    Color sideToMoveColor() {
      return Color.valueOf(sideToMove);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record BoardSpec(
      List<Integer> whiteMen,
      List<Integer> whiteKings,
      List<Integer> blackMen,
      List<Integer> blackKings) {

    BoardSpec {
      if (whiteMen == null) whiteMen = List.of();
      if (whiteKings == null) whiteKings = List.of();
      if (blackMen == null) blackMen = List.of();
      if (blackKings == null) blackKings = List.of();
    }
  }
}
