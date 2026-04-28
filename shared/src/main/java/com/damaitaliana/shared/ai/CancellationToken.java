package com.damaitaliana.shared.ai;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/**
 * Cooperative cancellation primitive (ADR-027).
 *
 * <p>The search code checks the token at every node so that an external request to stop — typically
 * {@code Future#cancel(true)} translated into {@link MutableCancellationToken#cancel()} by the
 * executor — propagates within ~200 ms. Implementations MUST be safe to read concurrently from the
 * searching thread.
 *
 * <p>Available factories:
 *
 * <ul>
 *   <li>{@link #never()} — never cancelled (singleton).
 *   <li>{@link #deadline(Instant)} / {@link #deadline(Instant, Clock)} — cancelled at/after a
 *       wall-clock instant.
 *   <li>{@link #composite(CancellationToken...)} — cancelled when any of the supplied tokens is
 *       cancelled (logical OR).
 * </ul>
 *
 * <p>The {@link MutableCancellationToken} concrete class supports manual cancellation via {@code
 * cancel()} and is what the executor (Task 2.8) uses behind the scenes.
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

  /**
   * Token that becomes cancelled at or after the given wall-clock {@code instant}, using the system
   * UTC clock.
   */
  static CancellationToken deadline(Instant instant) {
    return deadline(instant, Clock.systemUTC());
  }

  /**
   * Token that becomes cancelled at or after {@code instant} as observed through {@code clock}.
   * Useful in tests where time can be controlled with a {@link Clock#fixed} clock.
   */
  static CancellationToken deadline(Instant instant, Clock clock) {
    Objects.requireNonNull(instant, "instant");
    Objects.requireNonNull(clock, "clock");
    return () -> !clock.instant().isBefore(instant);
  }

  /**
   * Composite token: cancelled iff at least one of the supplied tokens is cancelled. Equivalent to
   * a logical OR over the inputs.
   *
   * @throws NullPointerException if any input is {@code null}.
   */
  static CancellationToken composite(CancellationToken... tokens) {
    Objects.requireNonNull(tokens, "tokens");
    for (CancellationToken t : tokens) {
      Objects.requireNonNull(t, "tokens[*]");
    }
    CancellationToken[] copy = Arrays.copyOf(tokens, tokens.length);
    return () -> {
      for (CancellationToken t : copy) {
        if (t.isCancelled()) {
          return true;
        }
      }
      return false;
    };
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
