package com.damaitaliana.server.auth;

/**
 * Thrown by {@link RefreshTokenService#verifyAndRotate(String)} and {@link
 * RefreshTokenService#revoke(String)} when the presented refresh token is structurally a valid JWT
 * (so {@link JwtVerificationException} would not fire) but is not acceptable for stateful reasons:
 * the corresponding row is missing from {@code refresh_tokens}, the row exists but is already
 * revoked, or the row exists but has expired since issuance.
 *
 * <p>The {@code GlobalExceptionHandler} added in Task 5.12 maps this to HTTP 401 Unauthorized. The
 * client-visible response carries the same generic body as {@link JwtVerificationException} —
 * "missing in DB" versus "already revoked" must not be observable from the outside (it would let an
 * attacker confirm a stolen token was actually issued).
 */
public class RefreshTokenInvalidException extends RuntimeException {

  public RefreshTokenInvalidException(String message) {
    super(message);
  }
}
