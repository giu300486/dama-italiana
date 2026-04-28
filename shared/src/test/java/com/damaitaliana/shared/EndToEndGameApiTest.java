package com.damaitaliana.shared;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Acceptance §16 Fase 1 — "Partita end-to-end via API Java pura senza UI né rete." Three scripted
 * games covering each terminal status: white-wins, black-wins, draw.
 */
class EndToEndGameApiTest {

  private final ItalianRuleEngine engine = new ItalianRuleEngine();

  @Test
  void whiteWinsByRemovingTheLastBlackPiece() {
    Board b =
        Board.empty()
            .with(new Square(3, 3), white(PieceKind.KING))
            .with(new Square(4, 4), black(PieceKind.MAN));
    GameState start = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);

    List<Move> options = engine.legalMoves(start);
    assertThat(options).hasSize(1);
    assertThat(options.get(0)).isInstanceOf(CaptureSequence.class);

    GameState end = engine.applyMove(start, options.get(0));
    assertThat(end.status()).isEqualTo(GameStatus.WHITE_WINS);
    assertThat(end.board().countPieces(Color.BLACK)).isZero();
    assertThat(end.history()).hasSize(1);
  }

  @Test
  void blackWinsByStalemattingWhite() {
    // Setup: white king at (7,7) cornered. Two black kings: one already at (6,6), one at (4,4)
    // about to move to (5,5). After black's move, white is stalemated.
    Board b =
        Board.empty()
            .with(new Square(7, 7), white(PieceKind.KING))
            .with(new Square(4, 4), black(PieceKind.KING))
            .with(new Square(6, 6), black(PieceKind.KING));
    GameState start = new GameState(b, Color.BLACK, 0, List.of(), GameStatus.ONGOING);

    SimpleMove blackMove = new SimpleMove(new Square(4, 4), new Square(5, 5));
    GameState end = engine.applyMove(start, blackMove);

    assertThat(end.status()).isEqualTo(GameStatus.BLACK_WINS);
    assertThat(engine.legalMoves(end)).isEmpty();
  }

  @Test
  void drawByFortyMoveRule() {
    // Two kings shuffling. Start near the limit (halfmoveClock = 78) so two king moves trip
    // the rule without dragging the test through dozens of plies.
    Board b =
        Board.empty()
            .with(new Square(3, 3), white(PieceKind.KING))
            .with(new Square(5, 5), black(PieceKind.KING));
    GameState s = new GameState(b, Color.WHITE, 78, List.of(), GameStatus.ONGOING);

    s = engine.applyMove(s, new SimpleMove(new Square(3, 3), new Square(2, 2)));
    assertThat(s.halfmoveClock()).isEqualTo(79);
    assertThat(s.status()).isEqualTo(GameStatus.ONGOING);

    s = engine.applyMove(s, new SimpleMove(new Square(5, 5), new Square(6, 6)));
    assertThat(s.halfmoveClock()).isEqualTo(80);
    assertThat(s.status()).isEqualTo(GameStatus.DRAW_FORTY_MOVES);
    assertThat(engine.legalMoves(s)).isEmpty();
  }

  // --- helpers ---

  private static Piece white(PieceKind k) {
    return new Piece(Color.WHITE, k);
  }

  private static Piece black(PieceKind k) {
    return new Piece(Color.BLACK, k);
  }
}
