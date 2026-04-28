package com.damaitaliana.shared.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import org.junit.jupiter.api.Test;

class EspertoAiTest {

  @Test
  void deterministicOnRepeatedCalls() {
    GameState start = GameState.initial();
    EspertoAi a = new EspertoAi();
    EspertoAi b = new EspertoAi();
    assertThat(a.chooseMove(start, CancellationToken.never()))
        .isEqualTo(b.chooseMove(start, CancellationToken.never()));
  }

  @Test
  void returnsLegalMoveOnInitialPosition() {
    GameState start = GameState.initial();
    Move m = new EspertoAi().chooseMove(start, CancellationToken.never());
    assertThat(new ItalianRuleEngine().legalMoves(start)).contains(m);
  }

  @Test
  void levelIsEsperto() {
    assertThat(new EspertoAi().level()).isEqualTo(AiLevel.ESPERTO);
  }
}
