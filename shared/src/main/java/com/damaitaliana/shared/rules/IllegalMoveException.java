package com.damaitaliana.shared.rules;

/** Thrown by {@link RuleEngine#applyMove} when the move is not legal in the current state. */
public class IllegalMoveException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public IllegalMoveException(String message) {
    super(message);
  }

  public IllegalMoveException(String message, Throwable cause) {
    super(message, cause);
  }
}
