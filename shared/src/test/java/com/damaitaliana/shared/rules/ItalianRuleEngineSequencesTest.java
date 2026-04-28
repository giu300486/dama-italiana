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

class ItalianRuleEngineSequencesTest {

  private final ItalianRuleEngine engine = new ItalianRuleEngine();

  // --- two-jump sequences ---

  @Test
  void whiteManTwoJumpStraightLine() {
    // White man at (1,1); blacks at (2,2) and (4,4); landings (3,3) and (5,5) free.
    Board b =
        Board.empty()
            .with(new Square(1, 1), white(PieceKind.MAN))
            .with(new Square(2, 2), black(PieceKind.MAN))
            .with(new Square(4, 4), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);

    List<Move> moves = engine.legalMoves(s);
    assertThat(moves).allMatch(Move::isCapture);
    assertThat(moves)
        .containsExactly(
            new CaptureSequence(
                new Square(1, 1),
                List.of(new Square(3, 3), new Square(5, 5)),
                List.of(new Square(2, 2), new Square(4, 4))));
  }

  @Test
  void whiteManTwoJumpZigzag() {
    // After landing at (3,3) the man takes (4,4) towards (5,5).
    // From (3,3) it could also go diagonally back-up to (1,5) but that requires capturing (2,4),
    // which is absent → only the first variant exists.
    Board b =
        Board.empty()
            .with(new Square(1, 1), white(PieceKind.MAN))
            .with(new Square(2, 2), black(PieceKind.MAN))
            .with(new Square(2, 4), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);

    assertThat(engine.legalMoves(s))
        .containsExactly(
            new CaptureSequence(
                new Square(1, 1),
                List.of(new Square(3, 3), new Square(1, 5)),
                List.of(new Square(2, 2), new Square(2, 4))));
  }

  @Test
  void blackManTwoJump() {
    // Black at (6,6); whites at (5,5) and (3,3); landings (4,4) and (2,2) free.
    Board b =
        Board.empty()
            .with(new Square(6, 6), black(PieceKind.MAN))
            .with(new Square(5, 5), white(PieceKind.MAN))
            .with(new Square(3, 3), white(PieceKind.MAN));
    GameState s = new GameState(b, Color.BLACK, 0, List.of(), GameStatus.ONGOING);
    assertThat(engine.legalMoves(s))
        .containsExactly(
            new CaptureSequence(
                new Square(6, 6),
                List.of(new Square(4, 4), new Square(2, 2)),
                List.of(new Square(5, 5), new Square(3, 3))));
  }

  // --- three-jump sequence (king) ---

  @Test
  void kingThreeJumpSequence() {
    // White king at (1,1). Three black men set up so that the king can chain:
    // (1,1) ⨯ (2,2) → (3,3) ⨯ (4,4) → (5,5) ⨯ (6,4) → (7,3).
    Board b =
        Board.empty()
            .with(new Square(1, 1), white(PieceKind.KING))
            .with(new Square(2, 2), black(PieceKind.MAN))
            .with(new Square(4, 4), black(PieceKind.MAN))
            .with(new Square(6, 4), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);

    List<Move> moves = engine.legalMoves(s);
    assertThat(moves).allMatch(Move::isCapture).hasSize(1);
    Move m = moves.get(0);
    assertThat(((CaptureSequence) m).captureCount()).isEqualTo(3);
    assertThat(m.to()).isEqualTo(new Square(7, 3));
  }

  // --- branching: multiple sequences from the same piece ---

  @Test
  void manWithBranchingHasMultipleSequences() {
    // White man at (1,1). After jumping (2,2) to (3,3), TWO continuations exist:
    //   - (3,3) ⨯ (4,4) → (5,5)
    //   - (3,3) ⨯ (2,4) → (1,5)
    Board b =
        Board.empty()
            .with(new Square(1, 1), white(PieceKind.MAN))
            .with(new Square(2, 2), black(PieceKind.MAN))
            .with(new Square(4, 4), black(PieceKind.MAN))
            .with(new Square(2, 4), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);

    List<Move> moves = engine.legalMoves(s);
    assertThat(moves).hasSize(2).allMatch(Move::isCapture);
    assertThat(moves)
        .extracting(Move::to)
        .containsExactlyInAnyOrder(new Square(5, 5), new Square(1, 5));
  }

  // --- promotion stops the sequence (SPEC §3.5) ---

  @Test
  void manReachingPromotionRowStopsTheSequence() {
    // White man at (1,5). Captures black at (2,6) → lands at (3,7) which is the promotion row.
    // Even if there were another capturable enemy after (3,7), the man cannot continue.
    Board b =
        Board.empty()
            .with(new Square(1, 5), white(PieceKind.MAN))
            .with(new Square(2, 6), black(PieceKind.MAN))
            // Decoy: a black man placed so that, were the rule absent, the white man could
            // continue: (3,7) ⨯ (4,6) → (5,5). With §3.5 the sequence stops at (3,7).
            .with(new Square(4, 6), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);

    List<Move> moves = engine.legalMoves(s);
    assertThat(moves)
        .containsExactly(
            new CaptureSequence(
                new Square(1, 5), List.of(new Square(3, 7)), List.of(new Square(2, 6))));
  }

  @Test
  void blackManReachingRank0StopsTheSequence() {
    Board b =
        Board.empty()
            .with(new Square(6, 2), black(PieceKind.MAN))
            .with(new Square(5, 1), white(PieceKind.MAN))
            // Decoy (would let the sequence continue):
            .with(new Square(3, 1), white(PieceKind.MAN));
    GameState s = new GameState(b, Color.BLACK, 0, List.of(), GameStatus.ONGOING);

    assertThat(engine.legalMoves(s))
        .containsExactly(
            new CaptureSequence(
                new Square(6, 2), List.of(new Square(4, 0)), List.of(new Square(5, 1))));
  }

  @Test
  void kingDoesNotStopAtAnyRowDuringSequence() {
    // A king crossing what would be a promotion row simply continues — kings are already
    // promoted and §3.5 does not apply to them.
    Board b =
        Board.empty()
            .with(new Square(1, 5), white(PieceKind.KING))
            .with(new Square(2, 6), black(PieceKind.MAN))
            .with(new Square(4, 6), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);

    List<Move> moves = engine.legalMoves(s);
    assertThat(moves).hasSize(1);
    assertThat(((CaptureSequence) moves.get(0)).captureCount()).isEqualTo(2);
    assertThat(moves.get(0).to()).isEqualTo(new Square(5, 5));
  }

  // --- same piece never jumped twice ---

  @Test
  void capturedPieceIsNotJumpedTwice() {
    // White king at (3,3); black at (4,4). The king could conceptually loop back over (4,4),
    // but the rule forbids re-capturing.
    Board b =
        Board.empty()
            .with(new Square(3, 3), white(PieceKind.KING))
            .with(new Square(4, 4), black(PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);

    List<Move> moves = engine.legalMoves(s);
    assertThat(moves).hasSize(1);
    assertThat(((CaptureSequence) moves.get(0)).captureCount()).isEqualTo(1);
  }

  // --- applyMove on multi-jump ---

  @Test
  void applyMoveOnMultiJumpRemovesAllCapturedPiecesInOrder() {
    Board b =
        Board.empty()
            .with(new Square(1, 1), white(PieceKind.MAN))
            .with(new Square(2, 2), black(PieceKind.MAN))
            .with(new Square(4, 4), black(PieceKind.MAN));
    GameState before = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    Move move = engine.legalMoves(before).get(0);

    GameState after = engine.applyMove(before, move);
    assertThat(after.board().at(new Square(2, 2))).isEmpty();
    assertThat(after.board().at(new Square(4, 4))).isEmpty();
    assertThat(after.board().at(new Square(5, 5))).contains(white(PieceKind.MAN));
    assertThat(after.board().countPieces(Color.BLACK)).isZero();
  }

  // --- helpers ---

  private static Piece white(PieceKind k) {
    return new Piece(Color.WHITE, k);
  }

  private static Piece black(PieceKind k) {
    return new Piece(Color.BLACK, k);
  }
}
