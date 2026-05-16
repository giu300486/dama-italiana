package com.damaitaliana.server.exception;

/**
 * Thrown when a registration attempt collides with an existing user (by username or email).
 *
 * <p>The {@code GlobalExceptionHandler} added in Task 5.12 maps this to HTTP 409 Conflict with a
 * Problem Details (RFC 7807) body. The exception message is logged server-side but not echoed to
 * the client (the response contains a generic "user already exists" detail to avoid leaking which
 * specific field collided — a minor information disclosure mitigation).
 */
public class UserAlreadyExistsException extends RuntimeException {

  public UserAlreadyExistsException(String message) {
    super(message);
  }
}
