package com.damaitaliana.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Persisted refresh token used by the auth flow to mint new access tokens without re-prompting the
 * user for credentials.
 *
 * <p>The {@code token_hash} column stores the SHA-256 hex digest (64 hex chars) of the raw JWT
 * refresh token issued to the client; the raw token itself is never persisted, so a database leak
 * cannot be replayed against the API. Lookup happens by hash, so verification requires the client
 * to present the original raw token, which is the only material capable of producing the same
 * digest.
 *
 * <p>Lifecycle: rows are inserted by {@code RefreshTokenService.issue}, marked revoked (i.e. set
 * {@link #revokedAt}) by either {@code verifyAndRotate} (on rotation) or {@code revoke} (on
 * logout), and removed via cascade if the parent user is deleted. Expired rows that are never
 * presented again are left in place until a future scheduled cleanup job processes them ({@code
 * idx_refresh_tokens_expires} backs that query).
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** Required by JPA. */
  protected RefreshTokenEntity() {}

  public RefreshTokenEntity(UserEntity user, String tokenHash, Instant expiresAt) {
    this.user = user;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  @PrePersist
  void onPrePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  public boolean isRevoked() {
    return revokedAt != null;
  }

  public boolean isExpired(Instant now) {
    return !expiresAt.isAfter(now);
  }

  public Long getId() {
    return id;
  }

  public UserEntity getUser() {
    return user;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(Instant revokedAt) {
    this.revokedAt = revokedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RefreshTokenEntity other)) {
      return false;
    }
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
