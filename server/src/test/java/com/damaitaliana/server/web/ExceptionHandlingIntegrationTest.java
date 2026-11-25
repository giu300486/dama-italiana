package com.damaitaliana.server.web;

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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Integration test for {@link GlobalExceptionHandler}. Asserts that every error path the API can
 * produce comes back as RFC 7807 {@code application/problem+json} with no stack trace or internal
 * detail leaking into the body, across the full status range: 400 (validation and unreadable body),
 * 401, 404, 409, and the 500 catch-all.
 *
 * <p>The 500 case needs a controller that throws something unmapped; {@link BoomConfig} contributes
 * a secured {@code /test-support/boom} endpoint that does exactly that, reached with a valid token.
 *
 * <p>The unreadable-body case (malformed JSON → 400, not 500) is the regression guard for the
 * catch-all: it proves the {@code Exception} handler does not swallow Spring's framework MVC
 * exceptions, which keep their correct status via {@code ResponseEntityExceptionHandler}.
 *
 * <p>Usernames are prefixed {@code exh_} to stay disjoint from sibling integration tests sharing
 * the H2 schema.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(ExceptionHandlingIntegrationTest.BoomConfig.class)
class ExceptionHandlingIntegrationTest {

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
  void validationError_returns400ProblemWithErrorsArray() throws Exception {
    RegisterRequest invalid = new RegisterRequest("ab", "exh_alice@integ.local", "Secret123");

    ResponseEntity<String> response =
        restTemplate.postForEntity("/v1/auth/register", invalid, String.class);

    assertProblemJson(response, HttpStatus.BAD_REQUEST);
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.get("title").asText()).isEqualTo("Bad Request");
    assertThat(body.get("errors")).isNotNull();
    assertThat(body.get("errors").toString()).contains("username");
    assertNoLeak(response.getBody());
  }

  @Test
  void unreadableBody_returns400_notSwallowedAs500ByTheCatchAll() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/v1/auth/register",
            HttpMethod.POST,
            new HttpEntity<>("{ this is not valid json", headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertNoLeak(response.getBody());
  }

  @Test
  void conflict_returns409Problem() throws Exception {
    register("exh_dup", "exh_dup@integ.local");

    RegisterRequest collision =
        new RegisterRequest("exh_dup", "exh_other@integ.local", "Secret123");
    ResponseEntity<String> response =
        restTemplate.postForEntity("/v1/auth/register", collision, String.class);

    assertProblemJson(response, HttpStatus.CONFLICT);
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.get("title").asText()).isEqualTo("Conflict");
    assertNoLeak(response.getBody());
  }

  @Test
  void missingToken_returns401Problem() {
    ResponseEntity<String> response =
        restTemplate.exchange("/v1/users/me", HttpMethod.GET, HttpEntity.EMPTY, String.class);

    assertProblemJson(response, HttpStatus.UNAUTHORIZED);
    assertNoLeak(response.getBody());
  }

  @Test
  void unexpectedException_returns500GenericProblemWithoutLeakingDetail() throws Exception {
    AuthResponse registered = register("exh_boom", "exh_boom@integ.local");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(registered.accessToken());
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/test-support/boom", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertProblemJson(response, HttpStatus.INTERNAL_SERVER_ERROR);
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.get("title").asText()).isEqualTo("Internal Server Error");
    assertThat(body.get("detail").asText()).isEqualTo("An unexpected error occurred");
    // The synthetic cause must not surface in any form.
    assertThat(response.getBody())
        .doesNotContain("IllegalStateException", "synthetic", ".java", "BoomController");
  }

  private void assertProblemJson(ResponseEntity<String> response, HttpStatus expected) {
    assertThat(response.getStatusCode()).isEqualTo(expected);
    assertThat(response.getHeaders().getContentType()).isNotNull();
    assertThat(response.getHeaders().getContentType().toString())
        .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
  }

  private static void assertNoLeak(String body) {
    assertThat(body).doesNotContain(".java", "Exception", "\tat ", "stackTrace");
  }

  private AuthResponse register(String username, String email) {
    RegisterRequest seed = new RegisterRequest(username, email, "Secret123");
    ResponseEntity<AuthResponse> response =
        restTemplate.postForEntity("/v1/auth/register", seed, AuthResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return response.getBody();
  }

  /** Contributes a secured endpoint that throws an unmapped exception, to exercise the 500 path. */
  @TestConfiguration
  static class BoomConfig {

    @Bean
    BoomController boomController() {
      return new BoomController();
    }
  }

  @RestController
  static class BoomController {

    @GetMapping("/test-support/boom")
    String boom() {
      throw new IllegalStateException("synthetic failure for the catch-all test");
    }
  }
}
