package com.damaitaliana.client.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiTurnServiceTest {

  private AiTurnService service;

  @BeforeEach
  void setUp() {
    service = new AiTurnService();
  }

  @AfterEach
  void tearDown() {
    service.close();
  }

  @Test
  void requestMoveReturnsLegalMoveAtPrincipiante() throws Exception {
    GameState state = GameState.initial();
    RandomGenerator rng = new SplittableRandom(42L);

    Move move = service.requestMove(state, AiLevel.PRINCIPIANTE, rng).get(2, TimeUnit.SECONDS);

    assertThat(move).isNotNull();
    List<Move> legal = new ItalianRuleEngine().legalMoves(state);
    assertThat(legal).contains(move);
  }

  @Test
  void terminalRootStateRequestReturnsNullMove() throws Exception {
    GameState terminal =
        new GameState(
            GameState.initial().board(),
            GameState.initial().sideToMove(),
            GameState.initial().halfmoveClock(),
            GameState.initial().history(),
            GameStatus.WHITE_WINS);

    Move move =
        service
            .requestMove(terminal, AiLevel.PRINCIPIANTE, new SplittableRandom(0L))
            .get(2, TimeUnit.SECONDS);

    assertThat(move).isNull();
  }

  @Test
  void parallelRequestsAreIndependent() throws Exception {
    GameState state = GameState.initial();
    RandomGenerator a = new SplittableRandom(1L);
    RandomGenerator b = new SplittableRandom(2L);

    CompletableFuture<Move> first = service.requestMove(state, AiLevel.PRINCIPIANTE, a);
    CompletableFuture<Move> second = service.requestMove(state, AiLevel.PRINCIPIANTE, b);

    Move m1 = first.get(2, TimeUnit.SECONDS);
    Move m2 = second.get(2, TimeUnit.SECONDS);
    assertThat(m1).isNotNull();
    assertThat(m2).isNotNull();
  }

  @Test
  void cancelAllStopsPendingRequestsGracefully() throws Exception {
    GameState state = GameState.initial();
    CompletableFuture<Move> future =
        service.requestMove(state, AiLevel.CAMPIONE, new SplittableRandom(42L));

    service.cancelAll();

    // Graceful cancel returns the best move so far instead of throwing — completes within
    // a reasonable time even though the level's nominal budget is 5 seconds.
    Move move = future.get(3, TimeUnit.SECONDS);
    assertThat(future.isDone()).isTrue();
    // The move can be null (nothing computed yet) or a legal move; both outcomes are acceptable
    // by VirtualThreadAiExecutor's graceful-cancel contract.
    if (move != null) {
      assertThat(new ItalianRuleEngine().legalMoves(state)).contains(move);
    }
  }

  @Test
  void closeCancelsAllAndShutsDownExecutor() throws InterruptedException {
    GameState state = GameState.initial();
    CompletableFuture<Move> future =
        service.requestMove(state, AiLevel.CAMPIONE, new SplittableRandom(42L));

    service.close();

    long deadline = System.currentTimeMillis() + 3_000;
    while (!future.isDone() && System.currentTimeMillis() < deadline) {
      Thread.sleep(20);
    }
    assertThat(future.isDone()).isTrue();
  }

  @Test
  void requestMoveRejectsNullArguments() {
    assertThatNullPointerException()
        .isThrownBy(
            () -> service.requestMove(null, AiLevel.PRINCIPIANTE, new SplittableRandom(0L)));
    assertThatNullPointerException()
        .isThrownBy(() -> service.requestMove(GameState.initial(), null, new SplittableRandom(0L)));
    assertThatNullPointerException()
        .isThrownBy(() -> service.requestMove(GameState.initial(), AiLevel.PRINCIPIANTE, null));
  }

  @Test
  void requestMoveCompletesWithoutBlockingCaller() {
    long start = System.nanoTime();
    CompletableFuture<Move> future =
        service.requestMove(GameState.initial(), AiLevel.CAMPIONE, new SplittableRandom(42L));
    long elapsed = (System.nanoTime() - start) / 1_000_000;

    // Returning the future should be near-immediate even at the highest level.
    assertThat(elapsed).isLessThan(200);
    future.cancel(true);
  }
}
