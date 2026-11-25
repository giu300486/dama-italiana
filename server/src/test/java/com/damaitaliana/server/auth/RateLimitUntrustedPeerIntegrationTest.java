package com.damaitaliana.server.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.server.dto.LoginRequest;
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
 * Integration test for {@link RateLimitFilter} when the connecting peer is not a trusted proxy: a
 * client that reaches the server directly must not be able to choose its rate-limit bucket through
 * {@code X-Forwarded-For} (ADR-047, F5 review).
 *
 * <p>The test client connects from loopback, which the shared configuration trusts as a proxy. This
 * class narrows {@code server.tomcat.remoteip.internal-proxies} to an address the client never uses
 * (TEST-NET-1), so loopback becomes an untrusted direct client; the distinct property set also
 * gives it its own application context and therefore its own counters.
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
      "dama.ratelimit.enabled=true",
      "dama.ratelimit.login-max-attempts=5",
      "dama.ratelimit.login-window-seconds=60",
      "server.tomcat.remoteip.internal-proxies=192\\.0\\.2\\.1"
    })
@ActiveProfiles("test")
class RateLimitUntrustedPeerIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void login_returns429_whenAnUntrustedClientRotatesForwardedFor() {
    for (int i = 0; i < 5; i++) {
      assertThat(loginForwardedFor("198.51.100." + i).getStatusCode())
          .isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    ResponseEntity<String> sixth = loginForwardedFor("198.51.100.99");
    assertThat(sixth.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }

  private ResponseEntity<String> loginForwardedFor(String forwardedFor) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Forwarded-For", forwardedFor);
    return restTemplate.exchange(
        "/v1/auth/login",
        HttpMethod.POST,
        new HttpEntity<>(new LoginRequest("rl_untrusted", "irrelevant"), headers),
        String.class);
  }
}
