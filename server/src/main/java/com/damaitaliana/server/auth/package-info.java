/**
 * Authentication: JWT issuing and verification, refresh token lifecycle and the auth REST
 * endpoints.
 *
 * <ul>
 *   <li>{@link com.damaitaliana.server.auth.AuthController}: {@code POST /v1/auth/register}, {@code
 *       /login}, {@code /refresh}, {@code /logout}.
 *   <li>{@link com.damaitaliana.server.auth.JwtService}: HS256 access and refresh tokens (ADR-045);
 *       every verification failure collapses into {@link
 *       com.damaitaliana.server.auth.JwtVerificationException}.
 *   <li>{@link com.damaitaliana.server.auth.RefreshTokenService}: persists only the SHA-256 hash of
 *       each refresh token, rotates under a pessimistic row lock and revokes on logout.
 *   <li>{@link com.damaitaliana.server.auth.JwtAuthenticationFilter} and {@link
 *       com.damaitaliana.server.auth.JwtAuthenticationEntryPoint}: stateless Bearer authentication
 *       with RFC 7807 401 responses.
 *   <li>{@link com.damaitaliana.server.auth.RateLimitFilter}: per-IP fixed-window limit on login
 *       and register (NFR-S-04, ADR-047).
 * </ul>
 */
package com.damaitaliana.server.auth;
