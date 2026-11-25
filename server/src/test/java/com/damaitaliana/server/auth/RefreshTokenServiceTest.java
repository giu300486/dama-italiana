package com.damaitaliana.server.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.server.auth.RefreshTokenService.IssuedRefreshToken;
import com.damaitaliana.server.auth.RefreshTokenService.RotatedTokens;
import com.damaitaliana.server.config.JwtProperties;
import com.damaitaliana.server.entity.RefreshTokenEntity;
import com.damaitaliana.server.entity.Role;
import com.damaitaliana.server.entity.UserEntity;
import com.damaitaliana.server.repository.RefreshTokenRepository;
import com.damaitaliana.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Slice test for {@link RefreshTokenService}. Runs in the {@code test} profile (H2 MySQL-compat
 * mode); the auth beans are pulled in via {@code @Import} since {@code @DataJpaTest} normally only
 * loads the JPA layer.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
@EnableConfigurationProperties(JwtProperties.class)
@Import({JwtService.class, RefreshTokenService.class})
class RefreshTokenServiceTest {

  @Autowired private RefreshTokenService service;
  @Autowired private RefreshTokenRepository tokenRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private JwtService jwtService;
  @Autowired private TestEntityManager entityManager;

  private UserEntity alice;

  @BeforeEach
  void setUp() {
    alice = userRepository.save(new UserEntity("alice", "alice@rttest.local", "hash", Role.USER));
    entityManager.flush();
  }

  @Test
  void issue_persistsEntityWithHashMatchingSha256OfRawToken() {
    IssuedRefreshToken issued = service.issue(alice);
    entityManager.flush();

    assertThat(issued.rawToken()).isNotBlank();
    assertThat(issued.entity().getId()).isNotNull();
    assertThat(issued.entity().getTokenHash())
        .isEqualTo(RefreshTokenService.sha256Hex(issued.rawToken()));
    assertThat(issued.entity().getRevokedAt()).isNull();
    assertThat(issued.entity().getExpiresAt()).isAfter(java.time.Instant.now());
    assertThat(issued.entity().getUser().getId()).isEqualTo(alice.getId());
  }

  @Test
  void verifyAndRotate_returnsNewPairAndMarksOldRevoked_whenTokenIsValid() {
    IssuedRefreshToken original = service.issue(alice);
    entityManager.flush();

    RotatedTokens rotated = service.verifyAndRotate(original.rawToken());
    entityManager.flush();
    entityManager.clear();

    assertThat(rotated.user().getId()).isEqualTo(alice.getId());
    assertThat(rotated.accessToken()).isNotBlank().isNotEqualTo(original.rawToken());
    assertThat(rotated.refreshToken()).isNotBlank().isNotEqualTo(original.rawToken());

    RefreshTokenEntity oldRow =
        tokenRepository.findByTokenHash(original.entity().getTokenHash()).orElseThrow();
    assertThat(oldRow.getRevokedAt()).isNotNull();

    RefreshTokenEntity newRow =
        tokenRepository
            .findByTokenHash(RefreshTokenService.sha256Hex(rotated.refreshToken()))
            .orElseThrow();
    assertThat(newRow.getRevokedAt()).isNull();
    assertThat(newRow.getUser().getId()).isEqualTo(alice.getId());
  }

  @Test
  void verifyAndRotate_throwsJwtVerification_whenAccessTokenPresentedAsRefresh() {
    String accessToken = jwtService.issueAccess(alice);

    assertThatThrownBy(() -> service.verifyAndRotate(accessToken))
        .isInstanceOf(JwtVerificationException.class)
        .hasMessageContaining("REFRESH");
  }

  @Test
  void verifyAndRotate_throwsRefreshTokenInvalid_onSecondAttemptWithSameToken() {
    IssuedRefreshToken original = service.issue(alice);
    entityManager.flush();

    service.verifyAndRotate(original.rawToken());
    entityManager.flush();

    assertThatThrownBy(() -> service.verifyAndRotate(original.rawToken()))
        .isInstanceOf(RefreshTokenInvalidException.class)
        .hasMessageContaining("revoked");
  }

  @Test
  void verifyAndRotate_throwsRefreshTokenInvalid_whenTokenWasNeverPersisted() {
    String orphanRefresh = jwtService.issueRefresh(alice);

    assertThatThrownBy(() -> service.verifyAndRotate(orphanRefresh))
        .isInstanceOf(RefreshTokenInvalidException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void revoke_marksTokenRevoked_whenTokenIsActive() {
    IssuedRefreshToken issued = service.issue(alice);
    entityManager.flush();

    service.revoke(issued.rawToken());
    entityManager.flush();
    entityManager.clear();

    RefreshTokenEntity row =
        tokenRepository.findByTokenHash(issued.entity().getTokenHash()).orElseThrow();
    assertThat(row.getRevokedAt()).isNotNull();
  }

  @Test
  void revoke_throwsRefreshTokenInvalid_onDoubleLogout() {
    IssuedRefreshToken issued = service.issue(alice);
    entityManager.flush();

    service.revoke(issued.rawToken());
    entityManager.flush();

    assertThatThrownBy(() -> service.revoke(issued.rawToken()))
        .isInstanceOf(RefreshTokenInvalidException.class)
        .hasMessageContaining("revoked");
  }

  @Test
  void revoke_throwsRefreshTokenInvalid_whenTokenWasNeverPersisted() {
    String orphanRefresh = jwtService.issueRefresh(alice);

    assertThatThrownBy(() -> service.revoke(orphanRefresh))
        .isInstanceOf(RefreshTokenInvalidException.class)
        .hasMessageContaining("not found");
  }
}
