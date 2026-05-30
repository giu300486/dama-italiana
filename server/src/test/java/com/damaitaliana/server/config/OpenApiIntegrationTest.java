package com.damaitaliana.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Integration test for the springdoc OpenAPI surface (A5.16, NFR-M-03). Verifies that {@code
 * /v3/api-docs} serves a 3.1 document carrying all five F5 endpoints, the {@code info} block from
 * {@link OpenApiConfig}, and the {@code bearerAuth} security scheme, and that the Swagger UI is
 * reachable. Both paths are whitelisted in {@code SecurityConfig}, so no token is needed.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OpenApiIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void apiDocs_isOpenApi31WithAllFiveEndpointsAndBearerScheme() throws Exception {
    ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isNotNull();
    assertThat(response.getHeaders().getContentType().toString())
        .startsWith(MediaType.APPLICATION_JSON_VALUE);

    JsonNode root = objectMapper.readTree(response.getBody());
    assertThat(root.path("openapi").asText()).startsWith("3.1");
    assertThat(root.path("info").path("title").asText()).isEqualTo("Dama Italiana Server API");
    assertThat(root.path("info").path("version").asText()).isEqualTo("0.5.0");

    JsonNode paths = root.path("paths");
    assertThat(paths.has("/v1/auth/register")).isTrue();
    assertThat(paths.has("/v1/auth/login")).isTrue();
    assertThat(paths.has("/v1/auth/refresh")).isTrue();
    assertThat(paths.has("/v1/auth/logout")).isTrue();
    assertThat(paths.has("/v1/users/me")).isTrue();

    JsonNode bearer = root.path("components").path("securitySchemes").path("bearerAuth");
    assertThat(bearer.isMissingNode()).isFalse();
    assertThat(bearer.path("type").asText()).isEqualTo("http");
    assertThat(bearer.path("scheme").asText()).isEqualTo("bearer");
    assertThat(bearer.path("bearerFormat").asText()).isEqualTo("JWT");

    // /v1/users/me references the scheme; /v1/auth/register does not.
    assertThat(paths.path("/v1/users/me").path("get").path("security").isArray()).isTrue();
    assertThat(paths.path("/v1/auth/register").path("post").has("security")).isFalse();
  }

  @Test
  void swaggerUi_isReachable() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/swagger-ui/index.html", String.class);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
  }
}
