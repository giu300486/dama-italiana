package com.damaitaliana.shared.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import java.time.Duration;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Performance tests for each AI level (NFR-P-02 — "IA Campione risponde entro 5 secondi in
 * posizioni di metà partita").
 *
 * <p>The test wraps {@link AiEngine#chooseMove} via {@link VirtualThreadAiExecutor} with the SPEC
 * default timeouts, then asserts the wall-clock result returns within the budget plus a 1.5×
 * tolerance (PLAN-fase-2 §7.7) to absorb CI cold-start jitter without masking real regressions.
 */
@Tag("performance")
class AiPerformanceTest {

  @Test
  void principianteRespondsWithinBudget() throws Exception {
    assertChooseMoveWithinBudget(
        new PrincipianteAi(new SplittableRandom(0L)), PrincipianteAi.DEFAULT_TIMEOUT);
  }

  @Test
  void espertoRespondsWithinBudget() throws Exception {
    assertChooseMoveWithinBudget(new EspertoAi(), EspertoAi.DEFAULT_TIMEOUT);
  }

  @Test
  void campioneRespondsWithinBudget() throws Exception {
    assertChooseMoveWithinBudget(new CampioneAi(), CampioneAi.DEFAULT_TIMEOUT);
  }

  private static void assertChooseMoveWithinBudget(AiEngine engine, Duration budget)
      throws Exception {
    try (VirtualThreadAiExecutor executor = new VirtualThreadAiExecutor()) {
      // Warm-up: discard the first call to amortise JIT and class-loading.
      executor
          .submitChooseMove(engine, GameState.initial(), budget)
          .get(budget.toMillis() * 2 + 1000, TimeUnit.MILLISECONDS);

      long t0 = System.nanoTime();
      VirtualThreadAiExecutor.Submission s =
          executor.submitChooseMove(engine, GameState.initial(), budget);
      Move m = s.get(budget.toMillis() * 2 + 1000, TimeUnit.MILLISECONDS);
      long elapsedMs = (System.nanoTime() - t0) / 1_000_000;

      assertThat(m).isNotNull();
      // Tolerance: budget * 1.5 (PLAN-fase-2 §7.7); plus 200 ms scheduling overhead.
      long ceilingMs = (long) (budget.toMillis() * 1.5) + 200;
      assertThat(elapsedMs).as("%s wall-clock", engine.level()).isLessThanOrEqualTo(ceilingMs);
    }
  }
}
