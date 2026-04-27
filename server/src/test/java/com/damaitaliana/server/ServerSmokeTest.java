package com.damaitaliana.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Smoke test for the server module build pipeline.
 *
 * <p>Verifies that the test classpath resolves (Spring Boot Test, AssertJ). Does NOT bootstrap a
 * Spring context — that requires the configuration classes added in Fase 5. Will be removed in Fase
 * 5 once real auth/match tests exist.
 */
class ServerSmokeTest {

  @Test
  void buildPipelineIsAlive() {
    assertThat("server").isEqualTo("server");
  }
}
