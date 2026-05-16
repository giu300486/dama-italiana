package com.damaitaliana.server.auth;

/**
 * Thrown by {@link JwtService#parse(String)} when a token cannot be trusted: invalid signature,
 * expired, structurally malformed, or the embedded {@code type} claim does not match what the
 * caller asked for.
 *
 * <p>The {@code GlobalExceptionHandler} added in Task 5.12 maps this to HTTP 401 Unauthorized with
 * a Problem Details body. The message is logged server-side; the response body is intentionally
 * generic to avoid leaking which specific verification step failed (a minor information disclosure
 * mitigation against attackers probing token validation).
 */
public class JwtVerificationException extends RuntimeException {

  public JwtVerificationException(String message) {
    super(message);
  }

  public JwtVerificationException(String message, Throwable cause) {
    super(message, cause);
  }
}
