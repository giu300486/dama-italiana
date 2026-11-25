package com.damaitaliana.server.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.server.dto.AuthResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@code POST /v1/auth/register}. Full Spring context, real H2 datastore,
 * TestRestTemplate against a random port. The DB is wiped before each test so they don't bleed into
 * each other (H2 in-memory has {@code DB_CLOSE_DELAY=-1} in {@code application-test.yml}).
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthRegisterIntegrationTest {

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
  void register_returns201WithTokens_whenInputIsValid() {
    RegisterRequest request =
        new RegisterRequest("reg_alice", "reg_alice@integ.local", "Secret123");

    ResponseEntity<AuthResponse> response =
        restTemplate.postForEntity("/v1/auth/register", request, AuthResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    AuthResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.user().username()).isEqualTo("reg_alice");
    assertThat(body.user().email()).isEqualTo("reg_alice@integ.local");
    assertThat(body.user().role()).isEqualTo("USER");
    assertThat(body.accessToken()).isNotBlank();
    assertThat(body.refreshToken()).isNotBlank().isNotEqualTo(body.accessToken());
    assertThat(body.tokenType()).isEqualTo("Bearer");
    assertThat(body.expiresIn()).isEqualTo(900L);

    assertThat(userRepository.findByUsername("reg_alice")).isPresent();
    assertThat(refreshTokenRepository.findAll()).hasSize(1);
  }

  @Test
  void register_returns409Generic_whenUsernameAlreadyExists() throws Exception {
    seedRegister("reg_alice", "reg_alice@integ.local", "Secret123");

    RegisterRequest collision =
        new RegisterRequest("reg_alice", "reg_different@integ.local", "Secret123");
    ResponseEntity<String> response =
        restTemplate.postForEntity("/v1/auth/register", collision, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getHeaders().getContentType().toString())
        .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.get("title").asText()).isEqualTo("Conflict");
    assertThat(body.get("detail").asText()).isEqualTo("Username or email already registered");
    // No leak: detail must NOT mention "username" or "email" specifically.
    assertThat(body.get("detail").asText().toLowerCase()).doesNotContain("username already");
  }

  @Test
  void register_returns409Generic_whenEmailAlreadyExists() throws Exception {
    seedRegister("reg_alice", "reg_alice@integ.local", "Secret123");

    RegisterRequest collision =
        new RegisterRequest("reg_bob", "reg_alice@integ.local", "Secret123");
    ResponseEntity<String> response =
        restTemplate.postForEntity("/v1/auth/register", collision, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.get("detail").asText()).isEqualTo("Username or email already registered");
  }

  @Test
  void register_returns400_whenUsernameIsTooShort() throws Exception {
    RegisterRequest invalid = new RegisterRequest("ab", "reg_alice@integ.local", "Secret123");

    ResponseEntity<String> response =
        restTemplate.postForEntity("/v1/auth/register", invalid, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.get("title").asText()).isEqualTo("Bad Request");
    assertThat(body.get("errors")).isNotNull();
    assertThat(body.get("errors").toString()).contains("username");
  }

  @Test
  void register_returns400_whenEmailIsMalformed() throws Exception {
    RegisterRequest invalid = new RegisterRequest("reg_alice", "not-an-email", "Secret123");

    ResponseEntity<String> response =
        restTemplate.postForEntity("/v1/auth/register", invalid, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.get("errors").toString()).contains("email");
  }

  @Test
  void register_returns400_whenPasswordIsTooWeak() throws Exception {
    // Long enough but no digit — fails the letter+digit Pattern.
    RegisterRequest invalid =
        new RegisterRequest("reg_alice", "reg_alice@integ.local", "noDigitsHere");

    ResponseEntity<String> response =
        restTemplate.postForEntity("/v1/auth/register", invalid, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.get("errors").toString()).contains("password");
  }

  private void seedRegister(String username, String email, String password) {
    RegisterRequest seed = new RegisterRequest(username, email, password);
    ResponseEntity<AuthResponse> response =
        restTemplate.postForEntity("/v1/auth/register", seed, AuthResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }
}
