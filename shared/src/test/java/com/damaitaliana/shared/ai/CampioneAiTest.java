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
import java.util.List;
import org.junit.jupiter.api.Test;

class CampioneAiTest {

  @Test
  void deterministicOnRepeatedCalls() {
    GameState start = GameState.initial();
    Move first = new CampioneAi().chooseMove(start, CancellationToken.never());
    Move second = new CampioneAi().chooseMove(start, CancellationToken.never());
    assertThat(first).isEqualTo(second);
  }

  @Test
  void findsForcedCaptureMate() {
    Board b =
        Board.empty()
            .with(new Square(4, 4), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(3, 5), new Piece(Color.BLACK, PieceKind.MAN));
    GameState state = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    Move chosen = new CampioneAi().chooseMove(state, CancellationToken.never());
    assertThat(chosen).isNotNull();
    assertThat(chosen.isCapture()).isTrue();
  }

  @Test
  void returnsLegalMoveOnInitialPosition() {
    GameState start = GameState.initial();
    Move m = new CampioneAi().chooseMove(start, CancellationToken.never());
    assertThat(new ItalianRuleEngine().legalMoves(start)).contains(m);
  }

  @Test
  void levelIsCampione() {
    assertThat(new CampioneAi().level()).isEqualTo(AiLevel.CAMPIONE);
  }

  @Test
  void clearTranspositionTableIsAvailable() {
    CampioneAi ai = new CampioneAi();
    ai.chooseMove(GameState.initial(), CancellationToken.never());
    ai.clearTranspositionTable(); // no exception
  }
}
