package com.damaitaliana.client.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.util.List;
import org.junit.jupiter.api.Test;

class MoveSelectorTest {

  private final RuleEngine engine = new ItalianRuleEngine();

  @Test
  void legalMovesFromInitialWhitePieceReturnsForwardMoves() {
    GameState state = GameState.initial();
    Square c3 = onlyWhiteSourceWithLegalMoves(state);

    List<Move> moves = MoveSelector.legalMovesFrom(state, engine, c3);

    assertThat(moves).isNotEmpty().allMatch(m -> m.from().equals(c3));
  }

  @Test
  void legalMovesFromEmptySquareReturnsEmpty() {
    GameState state = GameState.initial();
    Square emptySquare = new Square(0, 3);
    assertThat(state.board().at(emptySquare)).isEmpty();

    assertThat(MoveSelector.legalMovesFrom(state, engine, emptySquare)).isEmpty();
  }

  @Test
  void legalMovesFromOpponentPieceReturnsEmpty() {
    GameState state = GameState.initial();
    // White moves first; pick a black piece on rank 5 (which is dark since file+rank must be even).
    Square blackPiece = new Square(1, 5);
    assertThat(state.board().at(blackPiece)).isPresent();

    assertThat(MoveSelector.legalMovesFrom(state, engine, blackPiece)).isEmpty();
  }

  @Test
  void rejectsNullArguments() {
    GameState s = GameState.initial();
    Square sq = new Square(0, 0);
    assertThatNullPointerException()
        .isThrownBy(() -> MoveSelector.legalMovesFrom(null, engine, sq));
    assertThatNullPointerException().isThrownBy(() -> MoveSelector.legalMovesFrom(s, null, sq));
    assertThatNullPointerException().isThrownBy(() -> MoveSelector.legalMovesFrom(s, engine, null));
  }

  /**
   * Picks a white starting square that has at least one legal move (rank 2 — second-row pieces).
   */
  private Square onlyWhiteSourceWithLegalMoves(GameState state) {
    return engine.legalMoves(state).stream().findFirst().orElseThrow().from();
  }
}
