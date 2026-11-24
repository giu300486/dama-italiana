package com.damaitaliana.server.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for the health endpoint (A5.19). Observed through {@code GET /actuator/health}
 * without a token, as an orchestrator probe would call it.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HealthIntegrationTest {

  private static final String HEALTH_PATH = "/actuator/health";

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void healthIsPublicAndReportsDatabaseComponentWithoutDetails() {
    ResponseEntity<JsonNode> response = restTemplate.getForEntity(HEALTH_PATH, JsonNode.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode body = response.getBody();
    assertThat(body.path("status").asText()).isEqualTo("UP");
    JsonNode db = body.path("components").path("db");
    assertThat(db.path("status").asText()).isEqualTo("UP");
    assertThat(db.has("details")).isFalse();
  }
}
