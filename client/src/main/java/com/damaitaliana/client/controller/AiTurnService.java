package com.damaitaliana.client.controller;

import com.damaitaliana.shared.ai.AiEngine;
import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.ai.CampioneAi;
import com.damaitaliana.shared.ai.EspertoAi;
import com.damaitaliana.shared.ai.PrincipianteAi;
import com.damaitaliana.shared.ai.VirtualThreadAiExecutor;
import com.damaitaliana.shared.ai.VirtualThreadAiExecutor.Submission;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Schedules AI {@link Move} computations on the shared {@link VirtualThreadAiExecutor}, exposing
 * the result as a {@link CompletableFuture} the UI controllers can compose with their FX-thread
 * continuation. Reuses Fase 2 entirely — no AI logic lives here.
 *
 * <p>Per-level timeout comes from each level's {@code DEFAULT_TIMEOUT} (SPEC §12.2): Principiante
 * 500 ms, Esperto 2000 ms, Campione 5000 ms.
 *
 * <p>{@link #cancelAll()} signals a graceful shutdown to every pending submission (the search
 * returns its best move so far). {@link #close()} (called by Spring's {@code @PreDestroy}) goes
 * further with a hard cancel + executor shutdown so app exit doesn't leak virtual threads.
 */
@Component
public class AiTurnService implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(AiTurnService.class);

  private final VirtualThreadAiExecutor executor;
  private final Set<Submission> activeSubmissions = ConcurrentHashMap.newKeySet();

  public AiTurnService() {
    this(new VirtualThreadAiExecutor());
  }

  /** Visible for tests so they can swap in a controlled executor if needed. */
  AiTurnService(VirtualThreadAiExecutor executor) {
    this.executor = Objects.requireNonNull(executor, "executor");
  }

  /**
   * Asks the AI to choose a move from {@code state}. Returns a {@link CompletableFuture} that
   * completes with the chosen move (or {@code null} for a terminal root state, per {@link
   * AiEngine#chooseMove} contract). Cancelling the returned future, or invoking {@link
   * #cancelAll()}, propagates to a graceful stop on the underlying search.
   */
  public CompletableFuture<Move> requestMove(GameState state, AiLevel level, RandomGenerator rng) {
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(level, "level");
    Objects.requireNonNull(rng, "rng");

    AiEngine ai = AiEngine.forLevel(level, rng);
    Submission submission = executor.submitChooseMove(ai, state, defaultTimeout(level));
    activeSubmissions.add(submission);

    CompletableFuture<Move> future = new CompletableFuture<>();
    Thread.ofVirtual()
        .name("ai-turn-await")
        .start(
            () -> {
              try {
                future.complete(submission.get());
              } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                future.cancel(true);
              } catch (ExecutionException ex) {
                future.completeExceptionally(ex.getCause() != null ? ex.getCause() : ex);
              } catch (Exception ex) {
                future.completeExceptionally(ex);
              } finally {
                activeSubmissions.remove(submission);
              }
            });

    future.whenComplete(
        (move, ex) -> {
          if (future.isCancelled()) {
            submission.cancelGracefully();
          }
        });
    return future;
  }

  /** Best-effort graceful cancel of every pending request. */
  public void cancelAll() {
    activeSubmissions.forEach(Submission::cancelGracefully);
  }

  @Override
  @PreDestroy
  public void close() {
    activeSubmissions.forEach(Submission::cancelHard);
    activeSubmissions.clear();
    try {
      executor.close();
    } catch (RuntimeException ex) {
      log.warn("Error closing AI executor", ex);
    }
  }

  private static Duration defaultTimeout(AiLevel level) {
    return switch (level) {
      case PRINCIPIANTE -> PrincipianteAi.DEFAULT_TIMEOUT;
      case ESPERTO -> EspertoAi.DEFAULT_TIMEOUT;
      case CAMPIONE -> CampioneAi.DEFAULT_TIMEOUT;
    };
  }
}
