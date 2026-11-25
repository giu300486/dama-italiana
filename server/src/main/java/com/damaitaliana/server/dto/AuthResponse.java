package com.damaitaliana.server.dto;

/**
 * Response shape for authentication endpoints ({@code POST /v1/auth/register}, {@code
 * /v1/auth/login}, {@code /v1/auth/refresh}).
 *
 * <p>{@code accessToken} (TTL 15 min) and {@code refreshToken} (TTL 7 days) are JWTs issued by
 * {@code JwtService} (Task 5.5, not yet implemented). {@code tokenType} is always {@code "Bearer"}
 * for OAuth2-compatibility. {@code expiresIn} is the access-token lifetime in seconds (900),
 * provided so the client can schedule a proactive refresh.
 *
 * <p>In Task 5.4 the record exists as a shape only; the {@code accessToken}/{@code refreshToken}
 * fields will be populated by the controller in Task 5.8 once {@code JwtService} exists.
 */
public record AuthResponse(
    UserResponse user, String accessToken, String refreshToken, String tokenType, long expiresIn) {

  public static AuthResponse of(UserResponse user, String accessToken, String refreshToken) {
    return new AuthResponse(user, accessToken, refreshToken, "Bearer", 900L);
  }
}
