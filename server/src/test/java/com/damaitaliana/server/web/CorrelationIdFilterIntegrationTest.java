package com.damaitaliana.server.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Integration test for {@link CorrelationIdFilter} (A5.17): the id is generated when absent, echoed
 * when the client sends a valid one, replaced when the client value is malformed, present on error
 * responses written by the security chain, and visible in the MDC while the controller runs.
 *
 * <p>The MDC check uses a test-only controller under {@code /v1/auth/**} (whitelisted by {@code
 * SecurityConfig}) that returns {@code MDC.get("correlationId")}: the embedded server handles the
 * request on a single thread, so the value the controller reads is the one the filter put there.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(CorrelationIdFilterIntegrationTest.MdcProbeController.class)
class CorrelationIdFilterIntegrationTest {

  private static final String PROBE_PATH = "/v1/auth/_test_correlation_probe";

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void generatesUuid_whenHeaderAbsent() {
    ResponseEntity<String> response = get(PROBE_PATH, null);

    String id = response.getHeaders().getFirst(CorrelationIdFilter.HEADER);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(id).isNotBlank();
    assertThat(UUID.fromString(id).toString()).isEqualTo(id);
  }

  @Test
  void echoesClientValue_whenHeaderValid() {
    ResponseEntity<String> response = get(PROBE_PATH, "client-req_42.a");

    assertThat(response.getHeaders().getFirst(CorrelationIdFilter.HEADER))
        .isEqualTo("client-req_42.a");
  }

  @Test
  void replacesClientValue_whenHeaderMalformed() {
    String malformed = "bad id with spaces";

    ResponseEntity<String> response = get(PROBE_PATH, malformed);

    String id = response.getHeaders().getFirst(CorrelationIdFilter.HEADER);
    assertThat(id).isNotEqualTo(malformed);
    assertThat(UUID.fromString(id).toString()).isEqualTo(id);
  }

  @Test
  void replacesClientValue_whenHeaderTooLong() {
    String tooLong = "a".repeat(65);

    ResponseEntity<String> response = get(PROBE_PATH, tooLong);

    assertThat(response.getHeaders().getFirst(CorrelationIdFilter.HEADER)).isNotEqualTo(tooLong);
  }

  @Test
  void putsIdInMdc_whileControllerRuns() {
    ResponseEntity<String> response = get(PROBE_PATH, "mdc-probe-1");

    assertThat(response.getBody()).isEqualTo("mdc-probe-1");
  }

  @Test
  void echoesHeader_onUnauthorizedResponseFromSecurityChain() {
    ResponseEntity<String> response = get("/v1/users/me", "unauth-probe-1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getHeaders().getFirst(CorrelationIdFilter.HEADER))
        .isEqualTo("unauth-probe-1");
  }

  private ResponseEntity<String> get(String path, String correlationId) {
    HttpHeaders headers = new HttpHeaders();
    if (correlationId != null) {
      headers.add(CorrelationIdFilter.HEADER, correlationId);
    }
    return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
  }

  @RestController
  static class MdcProbeController {
    @GetMapping(PROBE_PATH)
    String probe() {
      return MDC.get(CorrelationIdFilter.MDC_KEY);
    }
  }
}
