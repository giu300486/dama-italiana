package com.damaitaliana.server.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.server.dto.AuthResponse;
import com.damaitaliana.server.dto.LoginRequest;
import com.damaitaliana.server.dto.RegisterRequest;
import com.damaitaliana.server.repository.RefreshTokenRepository;
import com.damaitaliana.server.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@code POST /v1/auth/login}. Exercises the register → login round-trip as
 * well as the two failure paths whose responses MUST be indistinguishable from the outside (unknown
 * user vs. wrong password — differentiating would leak which usernames are registered).
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthLoginIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void cleanDb() {
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void login_returns200WithTokensAndUpdatesLastLogin_afterPriorRegistration() {
    register("login_alice", "login_alice@integ.local", "Secret123");
    assertThat(userRepository.findByUsername("login_alice").orElseThrow().getLastLoginAt())
        .isNull();

    LoginRequest login = new LoginRequest("login_alice", "Secret123");
    ResponseEntity<AuthResponse> response =
        restTemplate.postForEntity("/v1/auth/login", login, AuthResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    AuthResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.user().username()).isEqualTo("login_alice");
    assertThat(body.user().lastLoginAt()).isNotNull();
    assertThat(body.accessToken()).isNotBlank();
    assertThat(body.refreshToken()).isNotBlank();

    assertThat(userRepository.findByUsername("login_alice").orElseThrow().getLastLoginAt())
        .isNotNull();
  }

  @Test
  void login_returns401Generic_whenPasswordIsWrong() throws Exception {
    register("login_alice", "login_alice@integ.local", "Secret123");

    LoginRequest wrong = new LoginRequest("login_alice", "WrongPassword1");
    ResponseEntity<String> response =
        restTemplate.postForEntity("/v1/auth/login", wrong, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.get("title").asText()).isEqualTo("Unauthorized");
    assertThat(body.get("detail").asText()).isEqualTo("Invalid username or password");
  }

  @Test
  void login_returns401SameGenericMessage_whenUserDoesNotExist() throws Exception {
    LoginRequest unknown = new LoginRequest("login_nobody", "Whatever1");
    ResponseEntity<String> response =
        restTemplate.postForEntity("/v1/auth/login", unknown, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.get("detail").asText()).isEqualTo("Invalid username or password");
  }

  @Test
  void login_returns400_whenUsernameIsBlank() {
    LoginRequest invalid = new LoginRequest("", "Secret123");
    ResponseEntity<String> response =
        restTemplate.postForEntity("/v1/auth/login", invalid, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  private void register(String username, String email, String password) {
    RegisterRequest seed = new RegisterRequest(username, email, password);
    ResponseEntity<AuthResponse> response =
        restTemplate.postForEntity("/v1/auth/register", seed, AuthResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }
}
