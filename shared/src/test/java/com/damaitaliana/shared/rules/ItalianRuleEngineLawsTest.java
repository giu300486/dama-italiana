package com.damaitaliana.shared.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.CaptureSequence;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.Square;
import java.util.List;
import org.junit.jupiter.api.Test;

/** SPEC §3.4 — four Italian laws of precedence: quantity, quality, king-precedence, first-king. */
class ItalianRuleEngineLawsTest {

  private final ItalianRuleEngine engine = new ItalianRuleEngine();

  // --- Law 1: quantity ---

  @Test
  void quantityLawDropsShorterCaptures() {
    // Two captures available: a 2-jump from a man and a 1-jump from another man.
    // Quantity law keeps only the 2-jump.
    Board b =
        Board.empty()
            .with(new Square(1, 1), white(PieceKind.MAN))
            .with(new Square(2, 2), black(PieceKind.MAN))
            .with(new Square(4, 4), black(PieceKind.MAN))
            // Second white man with a 1-capture option
            .with(new Square(5, 1), white(PieceKind.MAN))
            .with(new Square(6, 2), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);

    List<Move> moves = engine.legalMoves(s);
    assertThat(moves).hasSize(1);
    assertThat(((CaptureSequence) moves.get(0)).captureCount()).isEqualTo(2);
    assertThat(moves.get(0).from()).isEqualTo(new Square(1, 1));
    assertThat(moves.get(0).to()).isEqualTo(new Square(5, 5));
  }

  @Test
  void quantityLawKeepsBothWhenTied() {
    // Two independent 1-captures. Quantity tie → both legal (subject to subsequent laws).
    Board b =
        Board.empty()
            .with(new Square(1, 1), white(PieceKind.MAN))
            .with(new Square(2, 2), black(PieceKind.MAN))
            .with(new Square(5, 1), white(PieceKind.MAN))
            .with(new Square(6, 2), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s)).hasSize(2);
  }

  // --- Law 2: quality ---

  @Test
  void qualityLawKeepsCapturesWithMostKings() {
    // Two 2-capture sequences. Sequence A captures 1 king + 1 man. Sequence B captures 2 men.
    // Quality law keeps only A.
    // A: white king (1,1) over black king (2,2) → (3,3) over black man (4,4) → (5,5).
    // B: white king (5,7) over black man (4,6) → (3,5) over black man (2,6) → (1,7).
    Board b =
        Board.empty()
            .with(new Square(1, 1), white(PieceKind.KING))
            .with(new Square(2, 2), black(PieceKind.KING))
            .with(new Square(4, 4), black(PieceKind.MAN))
            .with(new Square(5, 7), white(PieceKind.KING))
            .with(new Square(4, 6), black(PieceKind.MAN))
            .with(new Square(2, 6), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);

    List<Move> moves = engine.legalMoves(s);
    assertThat(moves).hasSize(1);
    assertThat(moves.get(0).from()).isEqualTo(new Square(1, 1));
  }

  // --- Law 3: king precedence ---

  @Test
  void kingPrecedenceDropsManCapturesWhenAKingCanCapture() {
    // Both captures take 1 piece (men only). One performed by a man, one by a king. Law 3
    // forces the king's capture.
    Board b =
        Board.empty()
            .with(new Square(3, 1), white(PieceKind.MAN))
            .with(new Square(4, 2), black(PieceKind.MAN))
            .with(new Square(2, 4), white(PieceKind.KING))
            .with(new Square(3, 5), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);

    List<Move> moves = engine.legalMoves(s);
    assertThat(moves).hasSize(1);
    assertThat(moves.get(0).from()).isEqualTo(new Square(2, 4));
  }

  @Test
  void kingPrecedenceDoesNothingWhenAllCapturesAreFromKings() {
    Board b =
        Board.empty()
            .with(new Square(2, 4), white(PieceKind.KING))
            .with(new Square(3, 5), black(PieceKind.MAN))
            .with(new Square(6, 4), white(PieceKind.KING))
            .with(new Square(5, 5), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s)).hasSize(2);
  }

  // --- Law 4: first king ---

  @Test
  void firstKingLawForcesKingFirstAtSameTotalAndQuality() {
    // White king at (3,3) has two 2-capture options, each with one king + one man.
    // Path A (NE): over king (4,4) → (5,5) → over man (6,6) → (7,7). Captures king first.
    // Path B (SE): over man (4,2) → (5,1) → over king (6,2) → (7,3). Captures man first.
    // Same quantity (2), same kings (1). Law 4 keeps only A.
    Board b =
        Board.empty()
            .with(new Square(3, 3), white(PieceKind.KING))
            .with(new Square(4, 4), black(PieceKind.KING))
            .with(new Square(6, 6), black(PieceKind.MAN))
            .with(new Square(4, 2), black(PieceKind.MAN))
            .with(new Square(6, 2), black(PieceKind.KING));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);

    List<Move> moves = engine.legalMoves(s);
    assertThat(moves).hasSize(1);
    assertThat(moves.get(0).to()).isEqualTo(new Square(7, 7));
    CaptureSequence cs = (CaptureSequence) moves.get(0);
    assertThat(cs.captured()).containsExactly(new Square(4, 4), new Square(6, 6));
  }

  @Test
  void firstKingLawDoesNothingWhenNoneCapturesKingFirst() {
    // Two king-source 2-jump options, each captures a man first then a man. Law 4 doesn't fire.
    // Path A (SE-SE): (2,4) ⨯ (3,3) → (4,2) ⨯ (5,1) → (6,0).
    // Path B (NE then SE): (2,4) ⨯ (3,5) → (4,6) ⨯ (5,5) → (6,4).
    Board b =
        Board.empty()
            .with(new Square(2, 4), white(PieceKind.KING))
            .with(new Square(3, 3), black(PieceKind.MAN))
            .with(new Square(5, 1), black(PieceKind.MAN))
            .with(new Square(3, 5), black(PieceKind.MAN))
            .with(new Square(5, 5), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    List<Move> moves = engine.legalMoves(s);
    assertThat(moves).hasSize(2).allMatch(Move::isCapture);
    assertThat(moves).allMatch(m -> ((CaptureSequence) m).captureCount() == 2);
    assertThat(moves)
        .extracting(Move::to)
        .containsExactlyInAnyOrder(new Square(6, 0), new Square(6, 4));
  }

  // --- helpers ---

  private static Piece white(PieceKind k) {
    return new Piece(Color.WHITE, k);
  }

  private static Piece black(PieceKind k) {
    return new Piece(Color.BLACK, k);
  }
}
