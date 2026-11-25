package com.damaitaliana.server.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.server.dto.AuthResponse;
import com.damaitaliana.server.dto.RegisterRequest;
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
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@code POST /v1/auth/logout}. Verifies that logout revokes the refresh token
 * (so it can no longer be exchanged at {@code /refresh}) and that a second logout with the same
 * token is rejected with 401 rather than treated as an idempotent no-op — the API deliberately
 * surfaces the double-logout as a client bug.
 *
 * <p>Usernames are prefixed {@code logout_} to keep this class's rows disjoint from sibling
 * integration tests sharing the same H2 schema across the suite.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthLogoutIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @BeforeEach
  void cleanDb() {
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void logout_revokesRefreshToken_soItCanNoLongerBeExchanged() {
    AuthResponse registered = register("logout_alice", "logout_alice@integ.local", "Secret123");
    String refreshToken = registered.refreshToken();

    ResponseEntity<Void> logout = logout(refreshToken);
    assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    // The revoked refresh token is no longer accepted at /refresh.
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(refreshToken);
    ResponseEntity<String> refresh =
        restTemplate.exchange(
            "/v1/auth/refresh", HttpMethod.POST, new HttpEntity<>(headers), String.class);
    assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void logout_returns401_onSecondLogoutWithTheSameToken() {
    AuthResponse registered = register("logout_twice", "logout_twice@integ.local", "Secret123");
    String refreshToken = registered.refreshToken();

    assertThat(logout(refreshToken).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(refreshToken);
    ResponseEntity<String> second =
        restTemplate.exchange(
            "/v1/auth/logout", HttpMethod.POST, new HttpEntity<>(headers), String.class);
    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void logout_returns401_whenNoAuthorizationHeaderIsSent() {
    ResponseEntity<String> response =
        restTemplate.exchange("/v1/auth/logout", HttpMethod.POST, HttpEntity.EMPTY, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  private ResponseEntity<Void> logout(String refreshToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(refreshToken);
    return restTemplate.exchange(
        "/v1/auth/logout", HttpMethod.POST, new HttpEntity<>(headers), Void.class);
  }

  private AuthResponse register(String username, String email, String password) {
    RegisterRequest seed = new RegisterRequest(username, email, password);
    ResponseEntity<AuthResponse> response =
        restTemplate.postForEntity("/v1/auth/register", seed, AuthResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return response.getBody();
  }
}
