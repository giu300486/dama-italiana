package com.damaitaliana.shared.ai;

/**
 * Manually cancellable {@link CancellationToken} (ADR-027). Once {@link #cancel()} is called the
 * token stays cancelled forever; there is no way to reset it.
 *
 * <p>Used by {@code VirtualThreadAiExecutor} (Task 2.8) and by tests that need to interrupt a
 * search at a precise moment.
 */
public final class MutableCancellationToken implements CancellationToken {

  private volatile boolean cancelled;

  /** Requests cancellation. Idempotent. */
  public void cancel() {
    this.cancelled = true;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }
}
