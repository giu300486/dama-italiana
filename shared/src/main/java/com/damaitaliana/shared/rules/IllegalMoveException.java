package com.damaitaliana.shared.rules;

/**
 * Thrown by {@link RuleEngine#applyMove} when the move is not legal in the current state.
 *
 * <p>This is an <em>unchecked</em> exception. The {@code throws} clause on {@link
 * RuleEngine#applyMove} is documentation-only — callers MAY catch it but are not forced to.
 * Throwing as a {@link RuntimeException} keeps the engine API simple for both the in-process
 * single-player loop and the asynchronous server-side handlers added in later phases.
 */
public class IllegalMoveException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public IllegalMoveException(String message) {
    super(message);
  }

  public IllegalMoveException(String message, Throwable cause) {
    super(message, cause);
  }
}
