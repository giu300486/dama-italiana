package com.damaitaliana.shared.ai;

/**
 * Cooperative cancellation primitive (ADR-027).
 *
 * <p>The search code checks the token at every node so that an external request to stop — typically
 * {@code Future#cancel(true)} translated into {@link MutableCancellationToken#cancel()} by the
 * executor — propagates within ~200 ms. Implementations MUST be safe to read concurrently from the
 * searching thread.
 *
 * <p>Task 2.4 ships only the interface, the {@link #never()} singleton and the manually-cancellable
 * {@link MutableCancellationToken}. Task 2.5 will add deadline-based and composite tokens.
 */
public interface CancellationToken {

  /** Returns {@code true} if cancellation has been requested. */
  boolean isCancelled();

  /**
   * Throws a {@link SearchCancelledException} if the token is cancelled. Convenient for use at the
   * top of every search node.
   */
  default void throwIfCancelled() {
    if (isCancelled()) {
      throw new SearchCancelledException();
    }
  }

  /** A token that is never cancelled. Singleton-friendly. */
  static CancellationToken never() {
    return Never.INSTANCE;
  }

  /** Internal singleton for {@link #never()}. */
  final class Never implements CancellationToken {
    private static final Never INSTANCE = new Never();

    private Never() {}

    @Override
    public boolean isCancelled() {
      return false;
    }
  }
}
