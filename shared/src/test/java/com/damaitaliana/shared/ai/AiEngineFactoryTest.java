package com.damaitaliana.shared.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class AiEngineFactoryTest {

  private final SplittableRandom rng = new SplittableRandom(42);

  @Test
  void forLevelReturnsCorrectImplementation() {
    assertThat(AiEngine.forLevel(AiLevel.PRINCIPIANTE, rng)).isInstanceOf(PrincipianteAi.class);
    assertThat(AiEngine.forLevel(AiLevel.ESPERTO, rng)).isInstanceOf(EspertoAi.class);
    assertThat(AiEngine.forLevel(AiLevel.CAMPIONE, rng)).isInstanceOf(CampioneAi.class);
  }

  @Test
  void levelMethodMatchesEnum() {
    assertThat(AiEngine.forLevel(AiLevel.PRINCIPIANTE, rng).level())
        .isEqualTo(AiLevel.PRINCIPIANTE);
    assertThat(AiEngine.forLevel(AiLevel.ESPERTO, rng).level()).isEqualTo(AiLevel.ESPERTO);
    assertThat(AiEngine.forLevel(AiLevel.CAMPIONE, rng).level()).isEqualTo(AiLevel.CAMPIONE);
  }

  @Test
  void rejectsNullLevel() {
    assertThatThrownBy(() -> AiEngine.forLevel(null, rng)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsNullRng() {
    assertThatThrownBy(() -> AiEngine.forLevel(AiLevel.ESPERTO, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void allLevelsReturnLegalMoveOnInitialPosition() {
    GameState start = GameState.initial();
    var ruleEngine = new ItalianRuleEngine();
    for (AiLevel level : AiLevel.values()) {
      AiEngine engine = AiEngine.forLevel(level, new SplittableRandom(42));
      Move move = engine.chooseMove(start, CancellationToken.never());
      assertThat(move).as("move for level %s", level).isNotNull();
      assertThat(ruleEngine.legalMoves(start)).as("legality for level %s", level).contains(move);
    }
  }

  @Test
  void allLevelsReturnNullOnTerminalState() {
    GameState terminal =
        new GameState(
            com.damaitaliana.shared.domain.Board.empty(),
            com.damaitaliana.shared.domain.Color.WHITE,
            0,
            java.util.List.of(),
            com.damaitaliana.shared.domain.GameStatus.BLACK_WINS);
    for (AiLevel level : AiLevel.values()) {
      AiEngine engine = AiEngine.forLevel(level, new SplittableRandom(42));
      assertThat(engine.chooseMove(terminal, CancellationToken.never()))
          .as("level %s on terminal", level)
          .isNull();
    }
  }
}
