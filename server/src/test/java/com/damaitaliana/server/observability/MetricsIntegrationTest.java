package com.damaitaliana.server.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.server.dto.LoginRequest;
import com.damaitaliana.server.dto.RegisterRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for the Prometheus scrape endpoint (A5.18, NFR-O-01). Everything is observed
 * through {@code GET /actuator/prometheus}, called without a token like a real scraper would.
 *
 * <p>{@code @SpringBootTest} turns metric export off by default; {@link AutoConfigureObservability}
 * turns it back on so the scrape endpoint exists as in production.
 *
 * <p>The H2 database is shared with the other test classes, so assertions compare the values
 * scraped before and after each action instead of absolute numbers. Usernames carry the {@code
 * metr_} prefix plus a random suffix.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability(tracing = false)
@ActiveProfiles("test")
class MetricsIntegrationTest {

  private static final String SCRAPE_PATH = "/actuator/prometheus";
  private static final String LOGIN_SUCCESS = "auth_login_attempts_total{result=\"success\"}";
  private static final String LOGIN_FAILURE = "auth_login_attempts_total{result=\"failure\"}";
  private static final String REGISTER_ATTEMPTS = "auth_register_attempts_total";
  private static final String USERS_REGISTERED = "users_registered";

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void scrapeIsPublicAndExposesJvmMetrics() {
    ResponseEntity<String> response = restTemplate.getForEntity(SCRAPE_PATH, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("jvm_memory_used_bytes", "process_uptime_seconds");
  }

  @Test
  void loginAttemptsAreCountedBySuccessAndFailure() {
    String username = register();
    String before = scrape();

    assertThat(login(username, "Secret123")).isEqualTo(HttpStatus.OK);
    assertThat(login(username, "WrongPassword1")).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(login("metr_nobody", "Whatever1")).isEqualTo(HttpStatus.UNAUTHORIZED);

    String after = scrape();
    assertThat(value(after, LOGIN_SUCCESS) - value(before, LOGIN_SUCCESS)).isEqualTo(1.0);
    assertThat(value(after, LOGIN_FAILURE) - value(before, LOGIN_FAILURE)).isEqualTo(2.0);
  }

  @Test
  void registerAttemptsCountBothCreatedAndDuplicate() {
    String before = scrape();

    String username = register();
    assertThat(register(username)).isEqualTo(HttpStatus.CONFLICT);

    String after = scrape();
    assertThat(value(after, REGISTER_ATTEMPTS) - value(before, REGISTER_ATTEMPTS)).isEqualTo(2.0);
  }

  @Test
  void usersRegisteredGaugeFollowsTheNumberOfAccounts() {
    String before = scrape();

    register();

    String after = scrape();
    assertThat(value(after, USERS_REGISTERED) - value(before, USERS_REGISTERED)).isEqualTo(1.0);
  }

  @Test
  void httpRequestsArePublishedAsHistogram() {
    register();

    assertThat(scrape()).contains("http_server_requests_seconds_bucket{");
  }

  private String register() {
    String username = "metr_" + UUID.randomUUID().toString().substring(0, 8);
    assertThat(register(username)).isEqualTo(HttpStatus.CREATED);
    return username;
  }

  private HttpStatus register(String username) {
    RegisterRequest request = new RegisterRequest(username, username + "@integ.local", "Secret123");
    ResponseEntity<String> response =
        restTemplate.postForEntity("/v1/auth/register", request, String.class);
    return HttpStatus.valueOf(response.getStatusCode().value());
  }

  private HttpStatus login(String username, String password) {
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/v1/auth/login", new LoginRequest(username, password), String.class);
    return HttpStatus.valueOf(response.getStatusCode().value());
  }

  private String scrape() {
    ResponseEntity<String> response = restTemplate.getForEntity(SCRAPE_PATH, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return response.getBody();
  }

  /**
   * Value of one sample in the Prometheus text format, or 0 when the sample is absent (a counter
   * that has never been incremented). {@code sample} is the metric name plus its full label set,
   * exactly as the line starts.
   */
  private static double value(String scrape, String sample) {
    return scrape
        .lines()
        .filter(line -> line.startsWith(sample + " "))
        .map(line -> Double.parseDouble(line.substring(sample.length() + 1).trim()))
        .findFirst()
        .orElse(0.0);
  }
}
