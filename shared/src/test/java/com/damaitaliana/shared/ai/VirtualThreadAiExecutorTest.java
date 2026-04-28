package com.damaitaliana.shared.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import java.time.Duration;
import java.util.SplittableRandom;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class VirtualThreadAiExecutorTest {

  @Test
  void submittedTaskCompletesWithLegalMove() throws Exception {
    try (VirtualThreadAiExecutor executor = new VirtualThreadAiExecutor()) {
      AiEngine ai = new EspertoAi();
      VirtualThreadAiExecutor.Submission s =
          executor.submitChooseMove(ai, GameState.initial(), Duration.ofSeconds(2));
      Move m = s.get(3, TimeUnit.SECONDS);
      assertThat(m).isNotNull();
      assertThat(new ItalianRuleEngine().legalMoves(GameState.initial())).contains(m);
    }
  }

  @Test
  void respectsTinyTimeout() throws Exception {
    try (VirtualThreadAiExecutor executor = new VirtualThreadAiExecutor()) {
      AiEngine ai = new CampioneAi();
      long t0 = System.nanoTime();
      VirtualThreadAiExecutor.Submission s =
          executor.submitChooseMove(ai, GameState.initial(), Duration.ofMillis(150));
      Move m = s.get(2, TimeUnit.SECONDS);
      long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
      assertThat(m).isNotNull();
      // Deadline is 150 ms; allow plenty of headroom for IDS finishing iteration 1.
      assertThat(elapsedMs).isLessThan(2_000);
    }
  }

  @Test
  void gracefulCancelReturnsBestMoveSoFar() throws Exception {
    try (VirtualThreadAiExecutor executor = new VirtualThreadAiExecutor()) {
      AiEngine ai = new CampioneAi();
      VirtualThreadAiExecutor.Submission s =
          executor.submitChooseMove(ai, GameState.initial(), Duration.ofSeconds(30));
      Thread.sleep(50); // let depth 1 complete
      s.cancelGracefully();
      Move m = s.get(3, TimeUnit.SECONDS);
      assertThat(m).isNotNull();
      assertThat(new ItalianRuleEngine().legalMoves(GameState.initial())).contains(m);
    }
  }

  @Test
  void hardCancelMakesGetThrow() {
    try (VirtualThreadAiExecutor executor = new VirtualThreadAiExecutor()) {
      AiEngine ai = new CampioneAi();
      VirtualThreadAiExecutor.Submission s =
          executor.submitChooseMove(ai, GameState.initial(), Duration.ofSeconds(30));
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      assertThat(s.cancelHard()).isTrue();
      assertThatThrownBy(() -> s.get(3, TimeUnit.SECONDS))
          .isInstanceOf(CancellationException.class);
    }
  }

  @Test
  void multipleConcurrentSubmissionsResolveIndependently() throws Exception {
    try (VirtualThreadAiExecutor executor = new VirtualThreadAiExecutor()) {
      AiEngine espertoA = new EspertoAi();
      AiEngine espertoB = new PrincipianteAi(new SplittableRandom(7L));
      VirtualThreadAiExecutor.Submission a =
          executor.submitChooseMove(espertoA, GameState.initial(), Duration.ofSeconds(2));
      VirtualThreadAiExecutor.Submission b =
          executor.submitChooseMove(espertoB, GameState.initial(), Duration.ofSeconds(2));
      Move ma = a.get(3, TimeUnit.SECONDS);
      Move mb = b.get(3, TimeUnit.SECONDS);
      assertThat(ma).isNotNull();
      assertThat(mb).isNotNull();
    }
  }

  @Test
  void closedExecutorRejectsNewSubmissions() {
    VirtualThreadAiExecutor executor = new VirtualThreadAiExecutor();
    executor.close();
    assertThatThrownBy(
            () ->
                executor.submitChooseMove(
                    new EspertoAi(), GameState.initial(), Duration.ofSeconds(1)))
        .isInstanceOf(RejectedExecutionException.class);
  }

  @Test
  void rejectsNullArguments() {
    try (VirtualThreadAiExecutor executor = new VirtualThreadAiExecutor()) {
      assertThatThrownBy(
              () -> executor.submitChooseMove(null, GameState.initial(), Duration.ofSeconds(1)))
          .isInstanceOf(NullPointerException.class);
      assertThatThrownBy(
              () -> executor.submitChooseMove(new EspertoAi(), null, Duration.ofSeconds(1)))
          .isInstanceOf(NullPointerException.class);
      assertThatThrownBy(
              () -> executor.submitChooseMove(new EspertoAi(), GameState.initial(), null))
          .isInstanceOf(NullPointerException.class);
    }
  }

  @Test
  void rejectsNonPositiveTimeout() {
    try (VirtualThreadAiExecutor executor = new VirtualThreadAiExecutor()) {
      assertThatThrownBy(
              () -> executor.submitChooseMove(new EspertoAi(), GameState.initial(), Duration.ZERO))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(
              () ->
                  executor.submitChooseMove(
                      new EspertoAi(), GameState.initial(), Duration.ofMillis(-1)))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  void submissionExposesUnderlyingFuture() throws Exception {
    try (VirtualThreadAiExecutor executor = new VirtualThreadAiExecutor()) {
      VirtualThreadAiExecutor.Submission s =
          executor.submitChooseMove(new EspertoAi(), GameState.initial(), Duration.ofSeconds(2));
      Move m = s.get(3, TimeUnit.SECONDS);
      assertThat(m).isNotNull();
      assertThat(s.future().isDone()).isTrue();
      assertThat(s.future().isCancelled()).isFalse();
    }
  }
}
