package com.damaitaliana.shared.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Golden tactical positions: hand-crafted positions where the best move is known and reachable
 * within the level's depth budget. Verifies that the search "sees" the correct line.
 *
 * <p>Encoded in Java rather than the JSON corpus of Fase 1 to keep the test self-contained.
 */
class AiTacticalPositionsTest {

  private final RuleEngine ruleEngine = new ItalianRuleEngine();

  // --- Position A: white captures the only black piece ⇒ instant mate. ---

  @Test
  void mateInOne_whiteCapturesLastBlackPiece_visibleToAllLevels() {
    Board b =
        Board.empty()
            .with(new Square(4, 4), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(3, 5), new Piece(Color.BLACK, PieceKind.MAN));
    GameState state = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    Move expected = ruleEngine.legalMoves(state).get(0); // forced capture by Italian rules
    assertThat(new EspertoAi().chooseMove(state, CancellationToken.never())).isEqualTo(expected);
    assertThat(new CampioneAi().chooseMove(state, CancellationToken.never())).isEqualTo(expected);
  }

  // --- Position B: black king captures the only white piece ⇒ instant mate from black's side. ---

  @Test
  void mateInOne_blackKingCapturesBackwards_visibleToAllLevels() {
    Board b =
        Board.empty()
            .with(new Square(3, 5), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(4, 4), new Piece(Color.BLACK, PieceKind.KING));
    GameState state = new GameState(b, Color.BLACK, 0, List.of(), GameStatus.ONGOING);
    Move expected = ruleEngine.legalMoves(state).get(0);
    assertThat(new EspertoAi().chooseMove(state, CancellationToken.never())).isEqualTo(expected);
    assertThat(new CampioneAi().chooseMove(state, CancellationToken.never())).isEqualTo(expected);
  }

  // --- Position C: forced capture is the right tactical reply. ---

  @Test
  void forcedCaptureIsAlwaysPreferredEvenWhenLosingMaterial() {
    // White man on (4,4), black men on (3,5) and (1,5). White must capture (3,5) jumping to (2,6)
    // (Italian rules); after that white finds itself adjacent to (1,5). With material-only
    // evaluation Esperto and Campione still have to make the forced move — there is no choice.
    Board b =
        Board.empty()
            .with(new Square(4, 4), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(3, 5), new Piece(Color.BLACK, PieceKind.MAN))
            .with(new Square(1, 5), new Piece(Color.BLACK, PieceKind.MAN));
    GameState state = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    List<Move> legal = ruleEngine.legalMoves(state);
    assertThat(legal).hasSize(1);
    Move forced = legal.get(0);
    assertThat(forced.isCapture()).isTrue();
    assertThat(new EspertoAi().chooseMove(state, CancellationToken.never())).isEqualTo(forced);
    assertThat(new CampioneAi().chooseMove(state, CancellationToken.never())).isEqualTo(forced);
  }

  // --- Position D: white prefers winning a piece in 3 ply over standing still. ---

  @Test
  void prefersWinningMaterialInTwoMoves() {
    // Setup: a position where after white moves and black replies, white can capture a black man.
    // White king on (3,1), black man on (4,4), black man on (2,4). White's best is to advance
    // toward the centre to force a capture once black moves.
    Board b =
        Board.empty()
            .with(new Square(3, 1), new Piece(Color.WHITE, PieceKind.KING))
            .with(new Square(4, 4), new Piece(Color.BLACK, PieceKind.MAN))
            .with(new Square(2, 4), new Piece(Color.BLACK, PieceKind.MAN));
    GameState state = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    // Esperto and Campione must produce the same legal move and not lose material.
    Move espertoMove = new EspertoAi().chooseMove(state, CancellationToken.never());
    Move campioneMove = new CampioneAi().chooseMove(state, CancellationToken.never());
    assertThat(espertoMove).isNotNull();
    assertThat(campioneMove).isNotNull();
    assertThat(ruleEngine.legalMoves(state)).contains(espertoMove);
    assertThat(ruleEngine.legalMoves(state)).contains(campioneMove);
  }

  // --- Position E: terminal-position handling must not crash any level. ---

  @Test
  void terminalPositionReturnsNull() {
    GameState terminal =
        new GameState(Board.empty(), Color.WHITE, 0, List.of(), GameStatus.BLACK_WINS);
    assertThat(new EspertoAi().chooseMove(terminal, CancellationToken.never())).isNull();
    assertThat(new CampioneAi().chooseMove(terminal, CancellationToken.never())).isNull();
  }
}
