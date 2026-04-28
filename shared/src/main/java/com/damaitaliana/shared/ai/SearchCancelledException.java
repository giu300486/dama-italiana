package com.damaitaliana.shared.ai;

/**
 * Thrown by a search when a {@link CancellationToken} has been cancelled.
 *
 * <p>The search code catches this exception at the iterative-deepening boundary and falls back to
 * the best move computed in the previously completed iteration (Task 2.5). Below that boundary the
 * exception simply propagates: every call up the recursion stack is unwound without doing more
 * work.
 */
public final class SearchCancelledException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public SearchCancelledException() {
    super("search was cancelled");
  }
}
