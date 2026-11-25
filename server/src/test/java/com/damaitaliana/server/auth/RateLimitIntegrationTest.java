package com.damaitaliana.server.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.server.dto.LoginRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link RateLimitFilter}. The {@code test} profile disables rate limiting by
 * default, so this class re-enables it with short windows via {@code @SpringBootTest(properties)} —
 * the distinct property set also gives it an isolated application context, keeping the counters
 * away from the other auth integration tests.
 *
 * <p>Each test addresses the limiter from a distinct synthetic {@code X-Forwarded-For} IP so the
 * per-IP buckets stay disjoint regardless of method execution order (the filter bean, and therefore
 * its Caffeine caches, is shared across the class's methods). The header is honoured only because
 * the test client connects from loopback, the trusted proxy of the shared configuration; the IPs it
 * carries must not match that trusted-proxy regex, or Tomcat's {@code RemoteIpValve} skips them.
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
      "dama.ratelimit.enabled=true",
      "dama.ratelimit.login-max-attempts=5",
      "dama.ratelimit.login-window-seconds=2",
      "dama.ratelimit.register-max-attempts=3",
      "dama.ratelimit.register-window-seconds=2"
    })
@ActiveProfiles("test")
class RateLimitIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @BeforeEach
  void cleanDb() {
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void login_returns429_onTheSixthAttemptWithinTheWindow() {
    String ip = "10.10.0.1";

    // The first 5 attempts reach the controller (unknown user -> 401), none throttled.
    for (int i = 0; i < 5; i++) {
      assertThat(login(ip).getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    ResponseEntity<String> sixth = login(ip);
    assertThat(sixth.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(sixth.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("2");
  }

  @Test
  void register_returns429_onTheFourthAttemptWithinTheWindow() {
    String ip = "10.10.0.2";

    for (int i = 0; i < 3; i++) {
      assertThat(register(ip, "rl_reg_" + i).getStatusCode())
          .isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    ResponseEntity<String> fourth = register(ip, "rl_reg_overflow");
    assertThat(fourth.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(fourth.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("2");
  }

  @Test
  void login_counterResets_afterTheWindowExpires() throws InterruptedException {
    String ip = "10.10.0.3";

    for (int i = 0; i < 5; i++) {
      login(ip);
    }
    assertThat(login(ip).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

    // Wait out the 2 s window (plus margin); the bucket is evicted and the next attempt is fresh.
    Thread.sleep(3000);

    assertThat(login(ip).getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }

  @Test
  void login_returns429_whenTheClientRotatesASpoofedLeftmostForwardedForValue() {
    // A proxy that appends (nginx proxy_add_x_forwarded_for) keeps whatever the client sent on the
    // left; only the rightmost value is written by the trusted proxy. Rotating the spoofed part
    // must
    // not give the client a fresh counter. TEST-NET-3 addresses: public, never a trusted proxy.
    String realClient = "203.0.113.7";

    for (int i = 0; i < 5; i++) {
      String chain = "198.51.100." + i + ", " + realClient;
      assertThat(login(chain).getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    ResponseEntity<String> sixth = login("198.51.100.99, " + realClient);
    assertThat(sixth.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }

  private ResponseEntity<String> login(String clientIp) {
    return restTemplate.exchange(
        "/v1/auth/login",
        org.springframework.http.HttpMethod.POST,
        new HttpEntity<>(new LoginRequest("rl_unknown", "irrelevant"), forwardedFor(clientIp)),
        String.class);
  }

  private ResponseEntity<String> register(String clientIp, String username) {
    RegisterRequest body = new RegisterRequest(username, username + "@integ.local", "Secret123");
    return restTemplate.exchange(
        "/v1/auth/register",
        org.springframework.http.HttpMethod.POST,
        new HttpEntity<>(body, forwardedFor(clientIp)),
        String.class);
  }

  private static HttpHeaders forwardedFor(String clientIp) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Forwarded-For", clientIp);
    return headers;
  }
}
