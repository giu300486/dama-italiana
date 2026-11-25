package com.damaitaliana.server.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.server.auth.JwtService;
import com.damaitaliana.server.config.JwtProperties;
import com.damaitaliana.server.dto.AuthResponse;
import com.damaitaliana.server.dto.RegisterRequest;
import com.damaitaliana.server.dto.UserResponse;
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
 * Integration test for {@code GET /v1/users/me}. Covers the happy path (a valid access token yields
 * the caller's profile, with no password material leaking) plus the three rejection paths: no token
 * → 401, expired access token → 401 (both caught by the security filter), and a structurally valid
 * token whose user has since been deleted → 404.
 *
 * <p>Usernames are prefixed {@code me_} to keep this class's rows disjoint from sibling integration
 * tests sharing the same H2 schema across the suite.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UsersMeIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private JwtProperties jwtProperties;

  @BeforeEach
  void cleanDb() {
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void me_returns200WithProfile_whenAccessTokenIsValid() {
    AuthResponse registered = register("me_alice", "me_alice@integ.local", "Secret123");

    ResponseEntity<UserResponse> response = me(registered.accessToken(), UserResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    UserResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.username()).isEqualTo("me_alice");
    assertThat(body.email()).isEqualTo("me_alice@integ.local");
    assertThat(body.role()).isEqualTo("USER");
    assertThat(body.id()).isNotNull();
    assertThat(body.createdAt()).isNotNull();
  }

  @Test
  void me_doesNotLeakPasswordMaterial() {
    AuthResponse registered = register("me_noleak", "me_noleak@integ.local", "Secret123");

    ResponseEntity<String> raw = me(registered.accessToken(), String.class);

    assertThat(raw.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(raw.getBody())
        .doesNotContain("password", "passwordHash", "password_hash", "Secret123");
  }

  @Test
  void me_returns401_whenNoTokenIsSent() {
    ResponseEntity<String> response =
        restTemplate.exchange("/v1/users/me", HttpMethod.GET, HttpEntity.EMPTY, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void me_returns401_whenAccessTokenIsExpired() {
    register("me_expired", "me_expired@integ.local", "Secret123");
    UserEntity user = userRepository.findByUsername("me_expired").orElseThrow();
    String expiredAccess = expiringAccessService().issueAccess(user);

    ResponseEntity<String> response = me(expiredAccess, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void me_returns404_whenUserNoLongerExists() {
    AuthResponse registered = register("me_ghost", "me_ghost@integ.local", "Secret123");
    // The access token stays structurally valid and in-date; the row it points at is gone.
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();

    ResponseEntity<String> response = me(registered.accessToken(), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  private <T> ResponseEntity<T> me(String accessToken, Class<T> responseType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    return restTemplate.exchange(
        "/v1/users/me", HttpMethod.GET, new HttpEntity<>(headers), responseType);
  }

  private AuthResponse register(String username, String email, String password) {
    RegisterRequest seed = new RegisterRequest(username, email, password);
    ResponseEntity<AuthResponse> response =
        restTemplate.postForEntity("/v1/auth/register", seed, AuthResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return response.getBody();
  }

  /**
   * A throwaway {@link JwtService} sharing the live signing secret but with a negative access TTL,
   * so {@code issueAccess} mints a token that is already past its expiry the moment it is created.
   * Signed with the real key, it passes signature verification and fails purely on expiration.
   */
  private JwtService expiringAccessService() {
    JwtProperties expiring = new JwtProperties();
    expiring.setEnvironment(new MockEnvironment());
    expiring.setSecret(jwtProperties.getSecret());
    expiring.setAccessTtlSeconds(-1);
    expiring.setRefreshTtlSeconds(-1);
    return new JwtService(expiring);
  }
}
