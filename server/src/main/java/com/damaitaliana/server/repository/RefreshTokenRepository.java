package com.damaitaliana.server.repository;

import com.damaitaliana.server.entity.RefreshTokenEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link RefreshTokenEntity}.
 *
 * <p>The {@code ForUpdate} variant takes a {@code PESSIMISTIC_WRITE} row lock so that two
 * concurrent refresh attempts using the same token cannot both rotate it (one would succeed and
 * then mark revoked; the other would see {@code revoked_at IS NOT NULL} and reject). Without the
 * lock both transactions could read the row before either revokes it and both could mint new
 * tokens.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

  Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from RefreshTokenEntity t where t.tokenHash = :hash")
  Optional<RefreshTokenEntity> findByTokenHashForUpdate(@Param("hash") String tokenHash);

  /**
   * Marks every still-active refresh token owned by {@code userId} as revoked. Returns the number
   * of rows updated. Used by future "log out from all devices" features (F6+); kept here so the
   * repository surface is complete.
   */
  @Modifying
  @Query(
      "update RefreshTokenEntity t set t.revokedAt = :now"
          + " where t.user.id = :userId and t.revokedAt is null")
  int revokeAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);
}
