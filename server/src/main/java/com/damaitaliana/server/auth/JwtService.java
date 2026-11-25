package com.damaitaliana.server.auth;

import com.damaitaliana.server.config.JwtProperties;
import com.damaitaliana.server.entity.Role;
import com.damaitaliana.server.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies HS256-signed JWTs for the auth flow.
 *
 * <p>Two token kinds are minted: <em>access</em> tokens (short-lived, sent on every API call) and
 * <em>refresh</em> tokens (long-lived, exchanged for a new access token via {@code POST
 * /v1/auth/refresh}). The {@code type} claim discriminates them; {@link #parse(String)} surfaces it
 * as {@link JwtClaims.TokenType} so callers can refuse cross-use (e.g. an access token presented as
 * a refresh token, or vice versa).
 *
 * <p>Claims emitted: {@code sub=userId}, {@code username}, {@code role}, {@code iat}, {@code exp},
 * {@code type}. No PII is added beyond the username already carried as {@code sub}/{@code
 * username}; in particular no email, no password hash, no last-login timestamp.
 *
 * <p>Verification failures (bad signature, expiration, malformed envelope, unknown role, missing or
 * unexpected {@code type}) are all collapsed into a single {@link JwtVerificationException} so the
 * security filter has exactly one exception to map to HTTP 401 — no detail about which check failed
 * is reflected to the client.
 */
@Service
public class JwtService {

  private final JwtProperties properties;
  private final SecretKey signingKey;

  public JwtService(JwtProperties properties) {
    this.properties = properties;
    this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String issueAccess(UserEntity user) {
    return issue(user, JwtClaims.TokenType.ACCESS, properties.getAccessTtlSeconds());
  }

  public String issueRefresh(UserEntity user) {
    return issue(user, JwtClaims.TokenType.REFRESH, properties.getRefreshTtlSeconds());
  }

  /**
   * Parses {@code token}, verifies the signature, checks expiration, and confirms the {@code type}
   * claim is one of {@link JwtClaims.TokenType}. Returns the decoded claims on success; throws
   * {@link JwtVerificationException} on any failure.
   */
  public JwtClaims parse(String token) {
    Claims raw;
    try {
      raw = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    } catch (ExpiredJwtException e) {
      throw new JwtVerificationException("JWT expired", e);
    } catch (JwtException | IllegalArgumentException e) {
      throw new JwtVerificationException("JWT verification failed", e);
    }

    long userId;
    try {
      userId = Long.parseLong(raw.getSubject());
    } catch (NumberFormatException e) {
      throw new JwtVerificationException("JWT 'sub' is not a numeric user id", e);
    }

    String username = raw.get("username", String.class);
    if (username == null || username.isBlank()) {
      throw new JwtVerificationException("JWT 'username' claim missing");
    }

    Role role = readRole(raw);
    JwtClaims.TokenType type = readType(raw);

    return new JwtClaims(userId, username, role, type, raw.getExpiration().toInstant());
  }

  private String issue(UserEntity user, JwtClaims.TokenType type, int ttlSeconds) {
    Instant now = Instant.now();
    // A random jti makes every token globally unique even when issued in the same second
    // for the same subject (iat/exp are NumericDate, so they truncate to second precision).
    // Without it, a refresh-token rotation within one second would mint the same JWT bytes
    // twice and collide on the UNIQUE(token_hash) constraint in `refresh_tokens`.
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(String.valueOf(user.getId()))
        .claim("username", user.getUsername())
        .claim("role", user.getRole().name())
        .claim("type", type.name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(ttlSeconds)))
        .signWith(signingKey, Jwts.SIG.HS256)
        .compact();
  }

  private static Role readRole(Claims raw) {
    String roleClaim = raw.get("role", String.class);
    if (roleClaim == null) {
      throw new JwtVerificationException("JWT 'role' claim missing");
    }
    try {
      return Role.valueOf(roleClaim);
    } catch (IllegalArgumentException e) {
      throw new JwtVerificationException("JWT 'role' claim has unknown value: " + roleClaim, e);
    }
  }

  private static JwtClaims.TokenType readType(Claims raw) {
    String typeClaim = raw.get("type", String.class);
    if (typeClaim == null) {
      throw new JwtVerificationException("JWT 'type' claim missing");
    }
    try {
      return JwtClaims.TokenType.valueOf(typeClaim);
    } catch (IllegalArgumentException e) {
      throw new JwtVerificationException("JWT 'type' claim has unknown value: " + typeClaim, e);
    }
  }
}
