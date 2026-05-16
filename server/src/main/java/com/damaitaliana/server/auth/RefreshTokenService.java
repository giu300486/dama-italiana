package com.damaitaliana.server.auth;

import com.damaitaliana.server.entity.RefreshTokenEntity;
import com.damaitaliana.server.entity.UserEntity;
import com.damaitaliana.server.repository.RefreshTokenRepository;
import com.damaitaliana.server.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the stateful side of refresh tokens: persistence (so rotation can be enforced), revocation,
 * and the one-shot exchange of an old refresh token for a fresh access + refresh pair.
 *
 * <p>The raw refresh token (the JWT itself) is never persisted; only its SHA-256 hex digest goes
 * into {@code refresh_tokens.token_hash}. A database dump therefore cannot be replayed against the
 * API: an attacker would need to find a preimage of the digest, which SHA-256 makes infeasible.
 *
 * <p>Rotation under concurrency: {@link #verifyAndRotate(String)} uses {@link
 * RefreshTokenRepository#findByTokenHashForUpdate(String)} to take a row-level write lock. If two
 * clients race the same token, the second one waits for the first to commit and then sees {@code
 * revoked_at IS NOT NULL}, so only one rotation succeeds.
 */
@Service
@Transactional
public class RefreshTokenService {

  private final JwtService jwtService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;

  public RefreshTokenService(
      JwtService jwtService,
      RefreshTokenRepository refreshTokenRepository,
      UserRepository userRepository) {
    this.jwtService = jwtService;
    this.refreshTokenRepository = refreshTokenRepository;
    this.userRepository = userRepository;
  }

  public IssuedRefreshToken issue(UserEntity user) {
    String rawToken = jwtService.issueRefresh(user);
    JwtClaims claims = jwtService.parse(rawToken);
    String hash = sha256Hex(rawToken);
    RefreshTokenEntity entity = new RefreshTokenEntity(user, hash, claims.expiresAt());
    refreshTokenRepository.save(entity);
    return new IssuedRefreshToken(rawToken, entity);
  }

  public RotatedTokens verifyAndRotate(String rawToken) {
    JwtClaims claims = jwtService.parse(rawToken);
    if (claims.type() != JwtClaims.TokenType.REFRESH) {
      throw new JwtVerificationException("expected REFRESH token, got " + claims.type() + " token");
    }

    String hash = sha256Hex(rawToken);
    RefreshTokenEntity existing =
        refreshTokenRepository
            .findByTokenHashForUpdate(hash)
            .orElseThrow(() -> new RefreshTokenInvalidException("refresh token not found"));

    Instant now = Instant.now();
    if (existing.isRevoked()) {
      throw new RefreshTokenInvalidException("refresh token already revoked");
    }
    if (existing.isExpired(now)) {
      throw new RefreshTokenInvalidException("refresh token expired");
    }

    existing.setRevokedAt(now);

    UserEntity user =
        userRepository
            .findById(claims.userId())
            .orElseThrow(
                () -> new RefreshTokenInvalidException("user no longer exists for refresh token"));
    String newAccess = jwtService.issueAccess(user);
    IssuedRefreshToken newRefresh = issue(user);
    return new RotatedTokens(user, newAccess, newRefresh.rawToken());
  }

  public void revoke(String rawToken) {
    jwtService.parse(rawToken);
    String hash = sha256Hex(rawToken);
    RefreshTokenEntity existing =
        refreshTokenRepository
            .findByTokenHashForUpdate(hash)
            .orElseThrow(() -> new RefreshTokenInvalidException("refresh token not found"));
    if (existing.isRevoked()) {
      throw new RefreshTokenInvalidException("refresh token already revoked");
    }
    existing.setRevokedAt(Instant.now());
  }

  static String sha256Hex(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is mandated by every JCA provider since JDK 1.4 -- if it's missing the JVM is
      // broken in a way that warrants failing fast rather than continuing.
      throw new IllegalStateException("SHA-256 not available on this JVM", e);
    }
  }

  /** Outcome of {@link #issue(UserEntity)}: the raw token to hand back, and the persisted row. */
  public record IssuedRefreshToken(String rawToken, RefreshTokenEntity entity) {}

  /** Outcome of {@link #verifyAndRotate(String)}: who logged in, plus the new token pair. */
  public record RotatedTokens(UserEntity user, String accessToken, String refreshToken) {}
}
