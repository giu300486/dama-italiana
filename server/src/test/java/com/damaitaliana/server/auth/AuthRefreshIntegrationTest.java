package com.damaitaliana.server.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.server.config.JwtProperties;
import com.damaitaliana.server.dto.AuthResponse;
import com.damaitaliana.server.dto.RegisterRequest;
import com.damaitaliana.server.entity.UserEntity;
import com.damaitaliana.server.repository.RefreshTokenRepository;
import com.damaitaliana.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@code POST /v1/auth/refresh}. Covers the happy path (rotation issues a new
 * pair and the old refresh token becomes unusable) plus the three rejection paths the endpoint must
 * answer with 401: an expired refresh token (caught by the security filter), a structurally valid
 * but already-rotated token (caught in {@code RefreshTokenService}), and an access token presented
 * where a refresh token is expected (type mismatch).
 *
 * <p>Usernames are prefixed {@code refresh_} to keep this class's rows disjoint from sibling
 * integration tests sharing the same H2 schema across the suite.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthRefreshIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private JwtService jwtService;
  @Autowired private JwtProperties jwtProperties;

  @BeforeEach
  void cleanDb() {
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void refresh_rotatesTokens_andInvalidatesTheOldRefreshToken() {
    AuthResponse registered = register("refresh_alice", "refresh_alice@integ.local", "Secret123");
    String oldRefresh = registered.refreshToken();

    ResponseEntity<AuthResponse> rotated = refresh(oldRefresh, AuthResponse.class);

    assertThat(rotated.getStatusCode()).isEqualTo(HttpStatus.OK);
    AuthResponse body = rotated.getBody();
    assertThat(body).isNotNull();
    assertThat(body.user().username()).isEqualTo("refresh_alice");

    // The freshly minted access token is a valid, parseable ACCESS token for the same user.
    JwtClaims newAccess = jwtService.parse(body.accessToken());
    assertThat(newAccess.type()).isEqualTo(JwtClaims.TokenType.ACCESS);
    assertThat(newAccess.username()).isEqualTo("refresh_alice");

    // The new refresh token differs from the old one and is itself usable...
    assertThat(body.refreshToken()).isNotBlank().isNotEqualTo(oldRefresh);
    assertThat(refresh(body.refreshToken(), AuthResponse.class).getStatusCode())
        .isEqualTo(HttpStatus.OK);

    // ...while the rotated-out token is now revoked and rejected.
    assertThat(refresh(oldRefresh, String.class).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void refresh_returns401_whenRefreshTokenIsExpired() {
    register("refresh_expired", "refresh_expired@integ.local", "Secret123");
    UserEntity user = userRepository.findByUsername("refresh_expired").orElseThrow();
    String expiredRefresh = expiringRefreshService().issueRefresh(user);

    ResponseEntity<String> response = refresh(expiredRefresh, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void refresh_returns401_whenAnAccessTokenIsPresentedInsteadOfRefresh() {
    AuthResponse registered =
        register("refresh_mismatch", "refresh_mismatch@integ.local", "Secret123");

    ResponseEntity<String> response = refresh(registered.accessToken(), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void refresh_returns401_whenNoAuthorizationHeaderIsSent() {
    ResponseEntity<String> response =
        restTemplate.exchange("/v1/auth/refresh", HttpMethod.POST, HttpEntity.EMPTY, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  private <T> ResponseEntity<T> refresh(String refreshToken, Class<T> responseType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(refreshToken);
    return restTemplate.exchange(
        "/v1/auth/refresh", HttpMethod.POST, new HttpEntity<>(headers), responseType);
  }

  private AuthResponse register(String username, String email, String password) {
    RegisterRequest seed = new RegisterRequest(username, email, password);
    ResponseEntity<AuthResponse> response =
        restTemplate.postForEntity("/v1/auth/register", seed, AuthResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return response.getBody();
  }

  /**
   * A throwaway {@link JwtService} sharing the live signing secret but with a negative refresh TTL,
   * so {@code issueRefresh} mints a token that is already past its expiry the moment it is created.
   * Signed with the real key, it passes signature verification and fails purely on expiration.
   */
  private JwtService expiringRefreshService() {
    JwtProperties expiring = new JwtProperties();
    expiring.setEnvironment(new MockEnvironment());
    expiring.setSecret(jwtProperties.getSecret());
    expiring.setAccessTtlSeconds(-1);
    expiring.setRefreshTtlSeconds(-1);
    return new JwtService(expiring);
  }
}
