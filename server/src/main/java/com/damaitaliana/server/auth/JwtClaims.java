package com.damaitaliana.server.auth;

import com.damaitaliana.server.entity.Role;
import java.time.Instant;

/**
 * Subset of JWT claims the application cares about, decoded from a signed token.
 *
 * <p>Produced by {@link JwtService#parse(String)} after the signature, the expiration, and the
 * {@code type} claim have been validated. Consumers (the security filter in Task 5.7, the refresh
 * token service in Task 5.6) read {@link #userId()}, {@link #username()}, and {@link #role()}
 * directly without re-parsing the raw token.
 */
public record JwtClaims(
    long userId, String username, Role role, TokenType type, Instant expiresAt) {

  /** Discriminates short-lived API tokens from longer-lived refresh tokens. */
  public enum TokenType {
    ACCESS,
    REFRESH
  }
}
