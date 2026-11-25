package com.damaitaliana.server.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.server.config.JwtProperties;
import com.damaitaliana.server.entity.Role;
import com.damaitaliana.server.entity.UserEntity;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * Unit tests for {@link JwtService}. No Spring context, no DB; the service is wired by hand with a
 * pair of {@link JwtProperties} instances sharing different secrets, which lets the "wrong secret"
 * scenario be exercised without any HTTP plumbing.
 */
class JwtServiceTest {

  private static final String SECRET_A =
      "test-secret-A-not-for-prod-32bytes-padding-padding-padding-padding";
  private static final String SECRET_B =
      "test-secret-B-not-for-prod-32bytes-padding-padding-padding-padding";

  private JwtService service;
  private UserEntity alice;

  @BeforeEach
  void setUp() {
    service = buildService(SECRET_A, 900, 604800);
    alice = userFixture(42L, "alice", Role.USER);
  }

  @Test
  void parse_returnsAccessClaims_whenIssueAccessIsParsedBack() {
    String token = service.issueAccess(alice);

    JwtClaims claims = service.parse(token);

    assertThat(claims.userId()).isEqualTo(42L);
    assertThat(claims.username()).isEqualTo("alice");
    assertThat(claims.role()).isEqualTo(Role.USER);
    assertThat(claims.type()).isEqualTo(JwtClaims.TokenType.ACCESS);
    assertThat(claims.expiresAt()).isAfter(java.time.Instant.now());
  }

  @Test
  void parse_returnsRefreshClaims_whenIssueRefreshIsParsedBack() {
    UserEntity admin = userFixture(7L, "root", Role.SYSTEM_ADMIN);
    String token = service.issueRefresh(admin);

    JwtClaims claims = service.parse(token);

    assertThat(claims.userId()).isEqualTo(7L);
    assertThat(claims.username()).isEqualTo("root");
    assertThat(claims.role()).isEqualTo(Role.SYSTEM_ADMIN);
    assertThat(claims.type()).isEqualTo(JwtClaims.TokenType.REFRESH);
  }

  @Test
  void parse_throws_whenTokenIsAlreadyExpired() {
    JwtService expiringService = buildService(SECRET_A, -1, -1);
    String token = expiringService.issueAccess(alice);

    assertThatThrownBy(() -> expiringService.parse(token))
        .isInstanceOf(JwtVerificationException.class)
        .hasMessageContaining("expired");
  }

  @Test
  void parse_throws_whenSignatureIsTampered() {
    String token = service.issueAccess(alice);
    String tampered = flipLastCharacter(token);

    assertThatThrownBy(() -> service.parse(tampered)).isInstanceOf(JwtVerificationException.class);
  }

  @Test
  void parse_throws_whenTokenWasSignedWithDifferentSecret() {
    JwtService other = buildService(SECRET_B, 900, 604800);
    String foreignToken = other.issueAccess(alice);

    assertThatThrownBy(() -> service.parse(foreignToken))
        .isInstanceOf(JwtVerificationException.class);
  }

  @Test
  void issuedTokens_carryDistinctTypeClaim_soCallersCanRejectCrossUse() {
    String access = service.issueAccess(alice);
    String refresh = service.issueRefresh(alice);

    assertThat(service.parse(access).type()).isEqualTo(JwtClaims.TokenType.ACCESS);
    assertThat(service.parse(refresh).type()).isEqualTo(JwtClaims.TokenType.REFRESH);
  }

  @Test
  void parse_throws_whenTokenIsStructurallyMalformed() {
    assertThatThrownBy(() -> service.parse("not.a.jwt"))
        .isInstanceOf(JwtVerificationException.class);
  }

  private static JwtService buildService(String secret, int accessTtl, int refreshTtl) {
    JwtProperties props = new JwtProperties();
    props.setEnvironment(new MockEnvironment());
    props.setSecret(secret);
    props.setAccessTtlSeconds(accessTtl);
    props.setRefreshTtlSeconds(refreshTtl);
    return new JwtService(props);
  }

  private static UserEntity userFixture(long id, String username, Role role) {
    UserEntity user = new UserEntity(username, username + "@example.com", "hash", role);
    setField(user, "id", id);
    return user;
  }

  private static void setField(Object target, String name, Object value) {
    try {
      Field f = findField(target.getClass(), name);
      f.setAccessible(true);
      f.set(target, value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("failed to set field " + name, e);
    }
  }

  private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
    Class<?> c = cls;
    while (c != null) {
      try {
        return c.getDeclaredField(name);
      } catch (NoSuchFieldException ignored) {
        c = c.getSuperclass();
      }
    }
    throw new NoSuchFieldException(name);
  }

  private static String flipLastCharacter(String token) {
    char last = token.charAt(token.length() - 1);
    char flipped = (last == 'A') ? 'B' : 'A';
    return token.substring(0, token.length() - 1) + flipped;
  }
}
