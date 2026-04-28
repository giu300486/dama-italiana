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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class PrincipianteAiTest {

  /** RandomGenerator that always returns {@code nextDouble} = 0.99 → noise never triggers. */
  private static final RandomGenerator NO_NOISE_RNG =
      new RandomGenerator() {
        @Override
        public long nextLong() {
          return 0L;
        }

        @Override
        public double nextDouble() {
          return 0.99;
        }
      };

  @Test
  void noiseProbabilityIsApproximatelyTwentyFivePercent() {
    GameState start = GameState.initial();
    int trials = 200;
    int deviations = 0;

    PrincipianteAi noNoise = new PrincipianteAi(NO_NOISE_RNG);
    Move expected = noNoise.chooseMove(start, CancellationToken.never());

    for (int i = 0; i < trials; i++) {
      PrincipianteAi ai = new PrincipianteAi(new SplittableRandom(i + 1L));
      Move chosen = ai.chooseMove(start, CancellationToken.never());
      if (!chosen.equals(expected)) {
        deviations++;
      }
    }
    // 25% of 200 = 50; allow ±15 for binomial variance.
    assertThat(deviations).isBetween(35, 65);
  }

  @Test
  void singleLegalMovePathBypassesNoise() {
    Board b =
        Board.empty()
            .with(new Square(4, 4), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(3, 5), new Piece(Color.BLACK, PieceKind.MAN))
            .with(new Square(0, 0), new Piece(Color.BLACK, PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    var ruleEngine = new ItalianRuleEngine();
    assertThat(ruleEngine.legalMoves(s)).hasSize(1);

    RandomGenerator alwaysNoise =
        new RandomGenerator() {
          @Override
          public long nextLong() {
            return 0L;
          }

          @Override
          public double nextDouble() {
            return 0.0; // would always trigger noise, but Principiante must skip it
          }
        };
    PrincipianteAi ai = new PrincipianteAi(alwaysNoise);
    Move chosen = ai.chooseMove(s, CancellationToken.never());
    assertThat(chosen).isEqualTo(ruleEngine.legalMoves(s).get(0));
  }

  @Test
  void fixedSeedProducesReproducibleResult() {
    GameState start = GameState.initial();
    PrincipianteAi a = new PrincipianteAi(new SplittableRandom(42L));
    PrincipianteAi b = new PrincipianteAi(new SplittableRandom(42L));
    Move m1 = a.chooseMove(start, CancellationToken.never());
    Move m2 = b.chooseMove(start, CancellationToken.never());
    assertThat(m1).isEqualTo(m2);
  }

  @Test
  void noisyPickAlwaysReturnsLegalMove() {
    GameState start = GameState.initial();
    var ruleEngine = new ItalianRuleEngine();
    Map<Move, Integer> counts = new HashMap<>();
    for (int i = 0; i < 100; i++) {
      PrincipianteAi ai = new PrincipianteAi(new SplittableRandom(i));
      Move m = ai.chooseMove(start, CancellationToken.never());
      assertThat(ruleEngine.legalMoves(start)).contains(m);
      counts.merge(m, 1, Integer::sum);
    }
    assertThat(counts.size()).isGreaterThan(1);
  }

  @Test
  void levelIsPrincipiante() {
    assertThat(new PrincipianteAi(NO_NOISE_RNG).level()).isEqualTo(AiLevel.PRINCIPIANTE);
  }
}
