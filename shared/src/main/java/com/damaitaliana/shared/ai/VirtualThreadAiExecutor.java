package com.damaitaliana.shared.ai;

import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Virtual-thread executor for {@link AiEngine#chooseMove} (SPEC §12.2 — "L'IA gira su un virtual
 * thread (Java 21), cancellabile, con timeout. Non blocca la UI.").
 *
 * <p>Each submission spawns a dedicated virtual thread (cheap on Java 21) and wires three things to
 * the search:
 *
 * <ul>
 *   <li>A {@linkplain CancellationToken#deadline(Instant) deadline-based token} so the search stops
 *       when the per-move budget elapses.
 *   <li>A {@link MutableCancellationToken} the caller can flip via {@link
 *       Submission#cancelGracefully()} for cooperative early termination — the search winds down
 *       and {@code Submission#get()} returns the best move found so far.
 *   <li>A thread-interrupt-observing token so {@code Submission#cancelHard()} (which interrupts the
 *       virtual thread) also stops the search promptly.
 * </ul>
 *
 * <p>Hard-cancel makes {@code Future#get()} throw {@link
 * java.util.concurrent.CancellationException} — the standard JDK semantics. If the caller wants the
 * best move computed up to the point of cancellation, they should use {@link
 * Submission#cancelGracefully()} instead.
 *
 * <p>The executor is reusable. Closing it ({@link #close()}) shuts down the underlying executor
 * service via {@code shutdownNow}, attempting to interrupt all in-flight tasks.
 */
public final class VirtualThreadAiExecutor implements AutoCloseable {

  private final ExecutorService executor;

  public VirtualThreadAiExecutor() {
    this.executor = Executors.newVirtualThreadPerTaskExecutor();
  }

  /**
   * Submits {@code ai.chooseMove(state, …)} on a virtual thread with a {@code timeout} budget.
   *
   * @return a {@link Submission} carrying the underlying {@link Future} plus graceful-cancel hooks.
   * @throws java.util.concurrent.RejectedExecutionException if the executor has been closed.
   */
  public Submission submitChooseMove(AiEngine ai, GameState state, Duration timeout) {
    Objects.requireNonNull(ai, "ai");
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(timeout, "timeout");
    if (timeout.isNegative() || timeout.isZero()) {
      throw new IllegalArgumentException("timeout must be positive: " + timeout);
    }

    MutableCancellationToken graceful = new MutableCancellationToken();
    Instant deadline = Instant.now().plus(timeout);
    Future<Move> future =
        executor.submit(
            () -> {
              CancellationToken token =
                  CancellationToken.composite(
                      graceful,
                      CancellationToken.deadline(deadline),
                      () -> Thread.currentThread().isInterrupted());
              return ai.chooseMove(state, token);
            });
    return new Submission(future, graceful);
  }

  @Override
  public void close() {
    executor.shutdownNow();
  }

  /** Future-like wrapper exposing a graceful-cancel hook. */
  public static final class Submission {

    private final Future<Move> future;
    private final MutableCancellationToken graceful;

    Submission(Future<Move> future, MutableCancellationToken graceful) {
      this.future = future;
      this.graceful = graceful;
    }

    /** Underlying future. Useful for {@code isDone}, {@code isCancelled}, etc. */
    public Future<Move> future() {
      return future;
    }

    /**
     * Blocks until the search completes and returns the chosen move. Returns {@code null} only for
     * terminal root states.
     */
    public Move get() throws InterruptedException, ExecutionException {
      return future.get();
    }

    /** Bounded-wait variant of {@link #get()}. */
    public Move get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      return future.get(timeout, unit);
    }

    /**
     * Cooperative cancel: signals the search to wind down. {@link #get()} returns the best move
     * found so far instead of throwing. Idempotent.
     */
    public void cancelGracefully() {
      graceful.cancel();
    }

    /**
     * Hard cancel: interrupts the worker thread. {@link #get()} throws {@link
     * java.util.concurrent.CancellationException}. Returns the boolean from {@link
     * Future#cancel(boolean)}.
     */
    public boolean cancelHard() {
      return future.cancel(true);
    }
  }
}
